package io.jenkins.plugins.gitlabbranchsource;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import jenkins.scm.impl.trait.Discovery;
import org.gitlab4j.api.GitLabApiException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class CommitSkipTrait extends SCMSourceTrait {
    private static final Logger LOGGER = Logger.getLogger(CommitSkipTrait.class.getName());

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public CommitSkipTrait() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        context.withFilter(new ExcludeCommitSCMHeadFilter());
    }

    /**
     * Our descriptor.
     */
    @Symbol("gitLabCommitSkip")
    @Extension
    @Discovery
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.TagDiscoveryTrait_displayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitLabSCMSourceContext.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitLabSCMSource.class;
        }

    }

    private static class ExcludeCommitSCMHeadFilter extends SCMHeadFilter {

        @Override
        public boolean isExcluded(@NonNull SCMSourceRequest scmSourceRequest, @NonNull SCMHead scmHead) throws IOException, InterruptedException {
            if (scmSourceRequest instanceof GitLabSCMSourceRequest) {
                GitLabSCMSourceRequest gitlabSCMSource = ((GitLabSCMSourceRequest) scmSourceRequest);
                try {
                    String message = gitlabSCMSource.getGitLabApi().getCommitsApi().getCommit(
                        gitlabSCMSource.getGitlabProject().getId(),
                        scmHead.getName()
                    ).getMessage();
                    return containsSkipToken(message.toLowerCase());
                } catch (GitLabApiException e) {
                    LOGGER.log(Level.WARNING, "Unable to retrieve commit message", e);
                }
            }
            return false;
        }

        boolean containsSkipToken(String commitMsg) {
            return commitMsg.contains("[ci skip]") || commitMsg.contains("[skip ci]");
        }
    }
}
