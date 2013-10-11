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
        CLOUD_CONFIG("text/cloud-config", "cloudinit-cloud-config.txt");

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

    /**
     * Add a cloud-config file.
     *
     * @param content content of the file
     * @return the builder
     * @throws IllegalArgumentException a cloud-config file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#CLOUD_CONFIG
     */
    public CloudInitUserDataBuilder addCloudConfig(String content) {
        return addFile(FileType.CLOUD_CONFIG, content);
    }

    private CloudInitUserDataBuilder addFile(FileType fileType, String content) throws IllegalArgumentException {
        Preconditions.checkNotNull(fileType, "'fileType' can NOT be null");
        Preconditions.checkNotNull(content, "'content' can NOT be null");
        Preconditions.checkArgument(!alreadyAddedFileTypes.contains(fileType), "%s as already been added", fileType);
        alreadyAddedFileTypes.add(fileType);

        try {
            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            mimeBodyPart.setText(content, charset.name(), fileType.getMimeTextSubType());
            mimeBodyPart.setHeader("Content-Type", fileType.getMimeTextSubType() + "; charset=\"" + charset.name() + "\"; name=\"" + fileType.getFileName() + "\"");
            userDataMultipart.addBodyPart(mimeBodyPart);

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

        } catch (MessagingException | IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public String buildBase64UserData() {
        return Base64.encodeBytes(buildUserData().getBytes());
    }
}
