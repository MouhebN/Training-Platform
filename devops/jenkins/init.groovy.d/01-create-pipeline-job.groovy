import jenkins.model.Jenkins
import hudson.model.FreeStyleProject
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.GitSCM
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.UserRemoteConfig
import hudson.plugins.git.extensions.impl.CloneOption
import java.util.Collections

def jenkins = Jenkins.instance
def jobName = 'training-platform-ci'
def repoUrl = System.getenv('GITHUB_REPO_URL')?.trim()
def branch = (System.getenv('GITHUB_BRANCH') ?: 'main').trim()
def credId = (System.getenv('GITHUB_CREDENTIALS_ID') ?: 'github-credentials').trim()

if (jenkins.getItem(jobName) != null) {
    println "Job ${jobName} already exists — skip create (edit job in UI if you changed GitHub URL)"
    return
}

def job = jenkins.createProject(WorkflowJob, jobName)
job.setDescription('CI: Maven unit tests + SonarQube. Triggered by GitHub push (or manual / poll).')

if (repoUrl) {
    println "Creating Pipeline from SCM: ${repoUrl} (branch ${branch})"
    def remote = new UserRemoteConfig(repoUrl, null, null, credId)
    def scm = new GitSCM(
            Collections.singletonList(remote),
            Collections.singletonList(new BranchSpec("*/${branch}")),
            false,
            Collections.emptyList(),
            null,
            null,
            Collections.emptyList()
    )
    def definition = new CpsScmFlowDefinition(scm, 'Jenkinsfile')
    definition.setLightweight(true)
    job.setDefinition(definition)

    // Enable GitHub hook trigger for GITScm polling companion
    try {
        def triggerClass = Class.forName('com.cloudbees.jenkins.GitHubPushTrigger')
        def trigger = triggerClass.getDeclaredConstructor().newInstance()
        job.addTrigger(trigger)
        println 'GitHub push trigger enabled'
    } catch (Throwable t) {
        println "GitHub push trigger not available yet: ${t.message}"
    }
} else {
    println 'GITHUB_REPO_URL not set — creating local Pipeline from /workspace/Jenkinsfile'
    def jenkinsfile = new File('/workspace/Jenkinsfile')
    if (!jenkinsfile.exists()) {
        println 'Jenkinsfile not found — skip'
        return
    }
    job.setDefinition(new CpsFlowDefinition(jenkinsfile.getText('UTF-8'), true))
}

job.save()
println "Created pipeline job ${jobName}"
