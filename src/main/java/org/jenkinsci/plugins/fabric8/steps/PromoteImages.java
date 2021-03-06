/**
 * Copyright (C) Original Authors 2017
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.fabric8.steps;

import com.google.common.base.Strings;
import io.jenkins.functions.Argument;
import io.jenkins.functions.Step;
import org.jenkinsci.plugins.fabric8.CommandSupport;
import org.jenkinsci.plugins.fabric8.Fabric8Commands;
import org.jenkinsci.plugins.fabric8.StepExtension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Promote images
 */
@Step(displayName = "Promotes docker images to a docker registry like hub.docker.com")
public class PromoteImages extends CommandSupport implements Function<PromoteImages.Arguments, String> {
    public PromoteImages() {
    }

    public PromoteImages(CommandSupport parentStep) {
        super(parentStep);
    }

    @Override
    @Step
    public String apply(Arguments config) {
        final List<String> images = config.getImages();
        final String tag = config.getTag();
        final String org = config.getOrg();
        final String toRegistry = config.getToRegistry();

        if (Strings.isNullOrEmpty(tag)) {
            error("No tag specified for tagImages step for images " + images);
            return null;
        }
        if (isNullOrEmpty(org)) {
            error("Docker Organisation config missing so cannot promote images " + images);
            return null;
        }
        if (isNullOrEmpty(toRegistry)) {
            error("Promote To Docker Registry config missing so cannot promote images " + images);
            return null;
        }

        return container("docker", () -> {
            Fabric8Commands flow = new Fabric8Commands(PromoteImages.this);
            for (final String image : images) {
                if (flow.isSingleNode()) {
                    sh("docker tag " + org + "/" + image + ":" + tag + " " + toRegistry + "/" + org + "/" + image + ":" + tag);
                } else {
                    String registryHost = getDockerRegistryHost();
                    String registryPort = getDockerRegistryPort();

                    sh("docker pull " + registryHost + ":" + registryPort + "/fabric8/" + image + ":" + tag);
                    sh("docker tag " + registryHost + ":" + registryPort + "/" + org + "/" + image + ":" + tag + " " + toRegistry + "/" + org + "/" + image + ":" + tag);
                }

                retry(3, (Callable<String>) () -> {
                    sh("docker push " + toRegistry + "/" + org + "/" + image + ":" + tag);
                    return null;
                });
            }
            return null;
        });
    }

    public static class Arguments implements Serializable {
        private static final long serialVersionUID = 1L;

        @Argument
        private String tag = "";
        @Argument
        private String org = "";
        @Argument
        private String toRegistry = "";
        @Argument
        private List<String> images = new ArrayList<>();
        @Argument
        private String containerName = "clients";

        private StepExtension stepExtension;

        public Arguments() {
        }

        public Arguments(String tag, String org, String toRegistry, List<String> images, StepExtension stepExtension) {
            this.tag = tag;
            this.org = org;
            this.toRegistry = toRegistry;
            this.images = images;
            this.stepExtension = stepExtension;
        }

        /**
         * Returns why this step cannot be invoked or null if its valid
         */
        public String validate() {
            if (images != null && !images.isEmpty()) {
                if (Strings.isNullOrEmpty(org)) {
                    return "Cannot promote images " + images + " as missing the dockerOrganisation argument: " + this;
                }
                if (Strings.isNullOrEmpty(toRegistry)) {
                    return "Cannot promote images " + images + " as missing the promoteToDockerRegistry argument: " + this;
                }
            }
            return null;
        }

        public List<String> getImages() {
            return images;
        }

        public void setImages(List<String> images) {
            this.images = images;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getOrg() {
            return org;
        }

        public void setOrg(String org) {
            this.org = org;
        }

        public String getToRegistry() {
            return toRegistry;
        }

        public void setToRegistry(String toRegistry) {
            this.toRegistry = toRegistry;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public StepExtension getStepExtension() {
            return stepExtension;
        }

        public void setStepExtension(StepExtension stepExtension) {
            this.stepExtension = stepExtension;
        }
    }
}
