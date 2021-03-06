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
import org.jenkinsci.plugins.fabric8.StepExtension;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Tags docker images
 */
@Step(displayName = "Tags docker images")
public class TagImages extends CommandSupport implements Function<TagImages.Arguments, String> {

    public TagImages() {
    }

    public TagImages(CommandSupport parentStep) {
        super(parentStep);
    }

    public String apply(String tag, String... images) {
        return apply(tag, Arrays.asList(images));
    }

    public String apply(String tag, List<String> images) {
        return apply(new Arguments(tag, images, null));
    }

    @Override
    @Step
    public String apply(final Arguments args) {
        final List<String> images = args.getImages();
        final String tag = args.getTag();
        if (Strings.isNullOrEmpty(tag)) {
            error("No tag specified for tagImages step for images " + images);
            return null;
        }

        return container("docker", () -> {
            for (String image : images) {
                retry(3, () -> {
                    String registryHost = getDockerRegistryHost();
                    String registryPort = getDockerRegistryPort();

                    sh("docker pull " + registryHost + ":" + registryPort + "/fabric8/" + image + ":" + tag);
                    sh("docker tag  " + registryHost + ":" + registryPort + "/fabric8/" + image + ":" + tag + " docker.io/fabric8/" + image + ":" + tag);
                    sh("docker push docker.io/fabric8/" + image + ":" + tag);
                    return null;
                });
            }
            return null;
        });
    }

    public static class Arguments implements Serializable {
        private static final long serialVersionUID = 1L;

        @Argument
        @NotEmpty
        private String tag = "";
        @Argument
        private List<String> images = new ArrayList<>();
        @Argument
        private String containerName = "clients";

        private StepExtension stepExtension;

        public Arguments() {
        }

        public Arguments(String tag, List<String> images, StepExtension stepExtension) {
            this.tag = tag;
            this.images = images;
            this.stepExtension = stepExtension;
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
