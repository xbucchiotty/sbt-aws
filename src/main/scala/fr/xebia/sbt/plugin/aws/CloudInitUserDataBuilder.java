/*
 * Copyright 2008-2010 Xebia and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.xebia.sbt.plugin.aws;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import net.iharder.Base64;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Set;

/**
 * <p>
 * Build a <a href="https://help.ubuntu.com/community/CloudInit">CloudInit</a>
 * UserData file.
 * </p>
 * <p>
 * Sample:
 * </p>
 * <p/>
 * <pre>
 * <code>
 * // base64 encoded user data
 * String userData = CloudInitUserDataBuilder.start() //
 *                      .addShellScript(shellScript) //
 *                      .addCloudConfig(cloudConfig) //
 *                      .buildBase64UserData();
 *
 * RunInstancesRequest req = new RunInstancesRequest() //
 *                              .withInstanceType("t1.micro") //
 *                              .withImageId("ami-47cefa33") // amazon-linux in eu-west-1 region
 *                              .withMinCount(1).withMaxCount(1) //
 *                              .withSecurityGroupIds("default") //
 *                              .withKeyName("my-key") //
 *                              .withUserData(userData);
 *
 * RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest);
 * </code>
 * </pre>
 * <p>
 * Inspired by ubuntu-on-ec2 cloud-utils <a href=
 * "http://bazaar.launchpad.net/~ubuntu-on-ec2/ubuntu-on-ec2/cloud-utils/view/head:/write-mime-multipart"
 * >write-mime-multipart</a> python script.
 * </p>
 *
 * @author <a href="mailto:cyrille@cyrilleleclerc.com">Cyrille Le Clerc</a>
 * @see com.amazonaws.services.ec2.model.RunInstancesRequest#withUserData(String)
 */
public class CloudInitUserDataBuilder {

    /**
     * File types supported by CloudInit
     */
    public enum FileType {
        /**
         * <p>
         * This content is "boothook" data. It is stored in a file under
         * /var/lib/cloud and then executed immediately. This is the earliest
         * "hook" available. Note, that there is no mechanism provided for
         * running only once. The boothook must take care of this itself. It is
         * provided with the instance id in the environment variable
         * "INSTANCE_ID". This could be made use of to provide a
         * 'once-per-instance'
         * </p>
         */
        CLOUD_BOOTHOOK("text/cloud-boothook", "cloudinit-cloud-boothook.txt"), //
        /**
         * <p>
         * This content is "cloud-config" data. See the examples for a commented
         * example of supported config formats.
         * </p>
         * <p>
         * Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/cloud-config.txt"
         * >cloud-config.txt</a>
         * </p>
         */
        CLOUD_CONFIG("text/cloud-config", "cloudinit-cloud-config.txt"), //
        /**
         * <p>
         * This content is a "include" file. The file contains a list of urls,
         * one per line. Each of the URLs will be read, and their content will
         * be passed through this same set of rules. Ie, the content read from
         * the URL can be gzipped, mime-multi-part, or plain text
         * </p>
         * <p>
         * Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/include.txt"
         * >include.txt</a>
         * </p>
         */
        INCLUDE_URL("text/x-include-url", "cloudinit-x-include-url.txt"), //
        /**
         * <p>
         * This is a 'part-handler'. It will be written to a file in
         * /var/lib/cloud/data based on its filename. This must be python code
         * that contains a list_types method and a handle_type method. Once the
         * section is read the 'list_types' method will be called. It must
         * return a list of mime-types that this part-handler handlers.
         * </p>
         * <p>
         * Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/part-handler.txt"
         * >part-handler.txt</a>
         * </p>
         */
        PART_HANDLER("text/part-handler", "cloudinit-part-handler.txt"), //
        /**
         * <p>
         * Script will be executed at "rc.local-like" level during first boot.
         * rc.local-like means "very late in the boot sequence"
         * </p>
         * <p>
         * Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/user-script.txt"
         * >user-script.txt</a>
         * </p>
         */
        SHELL_SCRIPT("text/x-shellscript", "cloudinit-userdata-script.txt"), //
        /**
         * <p>
         * Content is placed into a file in /etc/init, and will be consumed by
         * upstart as any other upstart job.
         * </p>
         * <p>
         * Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/upstart-rclocal.txt"
         * >upstart-rclocal.txt</a>
         * </p>
         */
        UPSTART_JOB("text/upstart-job", "cloudinit-up()-job.txt");

        /**
         * Name of the file.
         */
        private final String fileName;
        /**
         * Mime Type of the file.
         */
        private final String mimeType;

        private FileType(String mimeType, String fileName) {
            this.mimeType = Preconditions.checkNotNull(mimeType);
            this.fileName = Preconditions.checkNotNull(fileName);
        }

        /**
         * @return name of the file
         */

        public String getFileName() {
            return fileName;
        }

        /**
         * e.g. "cloud-config" for "text/cloud-config"
         */

        public String getMimeTextSubType() {
            return getMimeType().substring("text/".length());
        }

        /**
         * e.g. "text/cloud-config"
         */

        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String toString() {
            return name() + "[" + mimeType + "]";
        }
    }

    /**
     * Initiates a new instance of the builder with the "UTF-8" charset.
     */
    public static CloudInitUserDataBuilder start() {
        return new CloudInitUserDataBuilder(Charsets.UTF_8);
    }

    /**
     * File types already added because cloud-init only supports one file of
     * each type.
     */
    private final Set<FileType> alreadyAddedFileTypes = Sets.newHashSet();

    /**
     * Charset used to generate the mime message.
     */
    private final Charset charset;

    /**
     * Mime message under creation
     */
    private final MimeMessage userDataMimeMessage;

    /**
     * Mime message's content under creation
     */
    private final MimeMultipart userDataMultipart;

    private CloudInitUserDataBuilder(Charset charset) {
        super();
        userDataMimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        userDataMultipart = new MimeMultipart();
        try {
            userDataMimeMessage.setContent(userDataMultipart);
        } catch (MessagingException e) {
            throw Throwables.propagate(e);
        }
        this.charset = Preconditions.checkNotNull(charset, "'charset' can NOT be null");
    }

    public CloudInitUserDataBuilder addCloudConfig(Readable cloudConfig) {
        return addFile(FileType.CLOUD_CONFIG, cloudConfig);
    }

    /**
     * Add a cloud-config file.
     *
     * @param cloudConfigFilePath classpath relative file path (e.g.
     *                            "com/my/company/cloud-config.txt")
     * @return the builder
     * @throws IllegalArgumentException a cloud-config file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#CLOUD_CONFIG
     */
    public CloudInitUserDataBuilder addCloudConfigFromFilePath(String cloudConfigFilePath) {

        InputStream cloudConfigAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(cloudConfigFilePath);
        Preconditions.checkNotNull(cloudConfigAsStream, "'" + cloudConfigFilePath + "' not found in path");
        Readable cloudConfig = new InputStreamReader(cloudConfigAsStream);

        return addFile(FileType.CLOUD_CONFIG, cloudConfig);
    }

    public CloudInitUserDataBuilder addFile(FileType fileType, Readable in) throws IllegalArgumentException {
        Preconditions.checkNotNull(fileType, "'fileType' can NOT be null");
        Preconditions.checkNotNull(in, "'in' can NOT be null");
        Preconditions.checkArgument(!alreadyAddedFileTypes.contains(fileType), "%s as already been added", fileType);
        alreadyAddedFileTypes.add(fileType);

        try {
            StringWriter sw = new StringWriter();
            CharStreams.copy(in, sw);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setText(sw.toString(), charset.name(), fileType.getMimeTextSubType());
            mimeBodyPart.setFileName(fileType.getFileName());
            userDataMultipart.addBodyPart(mimeBodyPart);

        } catch (IOException e) {
            throw Throwables.propagate(e);
        } catch (MessagingException e) {
            throw Throwables.propagate(e);
        }
        return this;
    }

    public String buildUserData() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            userDataMimeMessage.writeTo(baos);
            return new String(baos.toByteArray(), this.charset);

        } catch (MessagingException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public String buildBase64UserData() {
        return Base64.encodeBytes(buildUserData().getBytes());
    }
}
