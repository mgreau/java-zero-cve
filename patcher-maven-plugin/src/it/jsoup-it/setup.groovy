import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider
import org.apache.maven.scm.provider.git.repository.GitScmProviderRepository
import org.apache.maven.scm.ScmFileSet

// Clone jsoup repository
def provider = new GitExeScmProvider()
def repository = new GitScmProviderRepository("https://github.com/jhy/jsoup.git")
def fileSet = new ScmFileSet(new File(basedir))

// Checkout specific version for reproducible tests
provider.checkOut(repository, fileSet, "jsoup-1.15.2")