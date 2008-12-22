/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.octo.mtg.plugin;

import com.octo.mtg.plugin.tools.GrailsPluginProject;
import com.octo.mtg.plugin.tools.GrailsProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Validate consistency between Grails and Maven settings.
 *
 * @author <a href="mailto:aheritier@gmail.com">Arnaud HERITIER</a>
 * @version $Id$
 * @description Validate consistency between Grails (application.properties) and Maven (pom.xml) settings.
 * @goal validate
 * @phase validate
 * @requiresDependencyResolution runtime
 * @since 0.1
 */
public class MvnValidateMojo extends AbstractGrailsMojo {

    /**
     * The artifact id of the project.
     *
     * @parameter expression="${project.artifactId}"
     * @required
     * @readonly
     */
    private String artifactId;

    /**
     * The packaging of the project.
     *
     * @parameter expression="${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * The version id of the project.
     *
     * @parameter expression="${project.version}"
     * @required
     * @readonly
     */
    private String version;

    private static final String PLUGIN_PREFIX = "grails-";

    public void execute() throws MojoExecutionException, MojoFailureException {
        if ("grails-plugin".equals(packaging)) {
            validateGrailsPlugin();
        } else {
            validateGrailsApp();
        }
    }

    private void validateGrailsApp() throws MojoExecutionException, MojoFailureException {
        GrailsProject grailsProject;
        try {
            grailsProject = getGrailsServices().readProjectDescriptor();
        } catch (MojoExecutionException e) {
            getLog().info("No Grails application found - skipping validation.");
            return;
        }
        
        if (!artifactId.equals(grailsProject.getAppName())) {
            throw new MojoFailureException("app.name [" + grailsProject.getAppName() + "] in " +
                "application.properties is different of the artifactId [" + artifactId + "] in the pom.xml");
        }

        String pomVersion = version.trim();
        String grailsVersion = grailsProject.getAppVersion().trim();

        if (!grailsVersion.equals(pomVersion)) {
            throw new MojoFailureException("app.version [" + grailsVersion + "] in " +
                "application.properties is different of the version [" + pomVersion + "] in the pom.xml");
        }

        // We have to set the application version in grails settings for old versions
        if (grailsProject.getAppVersion() == null) {
            grailsProject.setAppVersion(GrailsProject.DEFAULT_APP_VERSION);
            getLog().warn("application.properties didn't contain an app.version property");
            getLog().warn("Setting to default value '" + grailsProject.getAppVersion() + "'.");

            getGrailsServices().writeProjectDescriptor(getBasedir(), grailsProject);
        }
    }

    private void validateGrailsPlugin() throws MojoExecutionException, MojoFailureException {

        GrailsPluginProject project = getGrailsServices().readGrailsPluginProject();

        String pluginName = project.getPluginName();

        if (artifactId.equals(pluginName)) {
            throw new MojoFailureException("The artifact id in pom.xml has to be the same as in " +
                project.getFileName() + " prefixed with '" + PLUGIN_PREFIX + "'. This is to avoid confustion when " +
                "the artifact is installed in the Maven repository.");
        }

        if (!artifactId.equals(PLUGIN_PREFIX + pluginName)) {
            throw new MojoFailureException("The plugin name in [" + pluginName + "] in " + project.getFileName() +
                " is not the expected " + PLUGIN_PREFIX + pluginName + ". Please correct the pom or the plugin " +
                "descriptor.");
        }

        String pomVersion = version.trim();
        String grailsVersion = project.getVersion();

        if (!grailsVersion.equals(pomVersion)) {
            throw new MojoFailureException("The version specified in the plugin configuration " +
                "[" + grailsVersion + "] in is different of the version [" + pomVersion + "] in the pom.xml");
        }
    }
}
