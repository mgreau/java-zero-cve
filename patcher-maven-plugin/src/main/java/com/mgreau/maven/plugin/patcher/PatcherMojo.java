package com.mgreau.maven.plugin.patcher;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.invoker.*;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.BasicScmManager;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.ScmTag;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Mojo(name = "apply-patch", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class PatcherMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    @Parameter(property = "githubRepoUrl", required = true)
    private String githubRepoUrl;

    @Parameter(property = "githubRepoTag", required = true)
    private String githubRepoTag;

    @Parameter(property = "patchVersion", required = true)
    private String patchVersion;

    @Parameter(defaultValue = "${project.build.directory}/source-project")
    private File targetProjectPath;

    @Parameter(defaultValue = "${project.build.directory}/patches")
    private File patchesDirectory;

    @Parameter(required = true)
    private List<PatcherConfig> patches;

    @Parameter(property = "continueOnError", defaultValue = "false")
    private boolean continueOnError;

    @Parameter(property = "skipFailedPatches", defaultValue = "false")
    private boolean skipFailedPatches;

    @Parameter(property = "backupPatched", defaultValue = "true")
    private boolean backupPatched;

    @Parameter(property = "buildCommand", defaultValue = "clean,install")
    private String buildCommand;

    @Parameter(property = "buildCommandSkipTests", defaultValue = "false")
    private boolean buildCommandSkipTests;

    private final CommandExecutor commandExecutor;

    private final ScmManager scmManager;

    public PatcherMojo() {
        this.commandExecutor = new CommandExecutor(getLog());
        this.scmManager = new BasicScmManager();
    }

    public void execute() throws MojoExecutionException {
        try {
            // Create necessary directories
            FileUtils.forceMkdir(targetProjectPath);
            FileUtils.forceMkdir(patchesDirectory);

            // Clone repository
            cloneRepository();

            // Sort patches by order
            patches.sort(Comparator.comparingInt(PatcherConfig::getOrder));

            // Create backup if enabled
            if (backupPatched) {
                createBackup();
            }

            // Download and apply patches
            final List<String> failedPatches = new ArrayList<>();
            for (PatcherConfig patch : patches) {
                try {
                    downloadPatch(patch);
                    applyPatch(patch);
                    getLog().info("Successfully applied patch: " + patch.getFilename());
                } catch (Exception e) {
                    String errorMessage = "Failed to apply patch " + patch.getFilename() + ": " + e.getMessage();
                    if (skipFailedPatches) {
                        getLog().warn(errorMessage);
                        failedPatches.add(patch.getFilename());
                        continue;
                    }
                    if (!continueOnError) {
                        throw new MojoExecutionException(errorMessage, e);
                    }
                    getLog().error(errorMessage);
                }
            }

            // Build the patched project
            buildPatchedProject();

            // Report failed patches
            if (!failedPatches.isEmpty()) {
                getLog().warn("The following patches failed to apply:");
                failedPatches.forEach(patch -> getLog().warn("- " + patch));
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute patch plugin", e);
        }
    }

    private void createBackup() throws IOException {
        final File backupDir = new File(targetProjectPath.getParentFile(), "backup-" + System.currentTimeMillis());
        FileUtils.copyDirectory(targetProjectPath, backupDir);
        getLog().info("Created backup at: " + backupDir.getAbsolutePath());
    }

    private void cloneRepository() throws MojoExecutionException {
        try {
            getLog().info("Cloning repository: " + githubRepoUrl);

            scmManager.setScmProvider( "git", new GitExeScmProvider() );
            
            // Create SCM repository
            final String scmUrl = "scm:git:" + githubRepoUrl + ".git";
            final ScmRepository repository = scmManager.makeScmRepository(scmUrl);
            
            // Configure checkout
            final ScmFileSet fileSet = new ScmFileSet(targetProjectPath);
            
            // Perform checkout
            final CheckOutScmResult result = scmManager.checkOut(repository, fileSet, new ScmTag(githubRepoTag));
            
            if (!result.isSuccess()) {
                throw new MojoExecutionException("Failed to clone repository: " + result.getProviderMessage());
            }
        } catch (ScmException e) {
            throw new MojoExecutionException("Error during repository clone", e);
        }
    }

    private void downloadPatch(final PatcherConfig patch) throws MojoExecutionException {
        try {
            getLog().info("Downloading patch from: " + patch.getUrl());
            
            final URL url = new URL(patch.getUrl());
            final File patchFile = new File(patchesDirectory, patch.getFilename());
            
            FileUtils.copyURLToFile(url, patchFile, 10000, 10000);
            
            if (!patchFile.exists()) {
                throw new MojoExecutionException("Failed to download patch file: " + patch.getFilename());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error downloading patch file: " + patch.getFilename(), e);
        }
    }

    private void applyPatch(final PatcherConfig patch) throws MojoExecutionException {
        try {
            getLog().info("Applying patch: " + patch.getFilename());
            if (patch.getDescription() != null) {
                getLog().info("Description: " + patch.getDescription());
            }
            
            final File patchFile = new File(patchesDirectory, patch.getFilename());
            
            final List<String> command = Arrays.asList(
                "git",
                "apply",
                "--whitespace=fix",
                "-p1",
                "--verbose",
                patchFile.getAbsolutePath()
            );
            
            commandExecutor.executeCommand(command, targetProjectPath);
            
        } catch (Exception e) {
            throw new MojoExecutionException("Error applying patch: " + patch.getFilename(), e);
        }
    }

    private void buildPatchedProject() throws MojoExecutionException {
        try {
            getLog().info("Building patched project with version: " + patchVersion);
            
            // First, set the new version
            final InvocationRequest setVersionRequest = new DefaultInvocationRequest();
            setVersionRequest.setBaseDirectory(targetProjectPath);
            setVersionRequest.addArgs(Arrays.asList(
                "org.codehaus.mojo:versions-maven-plugin:2.16.1:set",
                "-DnewVersion=" + patchVersion
            ));
            
            // Then, build the project
            final InvocationRequest buildRequest = new DefaultInvocationRequest();
            buildRequest.setBaseDirectory(targetProjectPath);
            buildRequest.addArgs(Arrays.asList(buildCommand.split(",")));
            final Properties properties = new Properties();
            properties.setProperty("skipTests", String.valueOf(buildCommandSkipTests));
            buildRequest.setProperties(properties);
            
            // Execute both requests
            final Invoker invoker = new DefaultInvoker();
            
            final InvocationResult setVersionResult = invoker.execute(setVersionRequest);
            if (setVersionResult.getExitCode() != 0) {
                throw new MojoExecutionException("Failed to set version. Exit code: " + setVersionResult.getExitCode());
            }
            
            final InvocationResult buildResult = invoker.execute(buildRequest);
            if (buildResult.getExitCode() != 0) {
                throw new MojoExecutionException("Failed to build project. Exit code: " + buildResult.getExitCode());
            }
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Error building patched project", e);
        }
    }
}