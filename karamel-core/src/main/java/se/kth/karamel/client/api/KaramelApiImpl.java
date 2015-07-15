/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.karamel.client.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.util.List;
import se.kth.karamel.backend.ClusterDefinitionService;
import se.kth.karamel.backend.ClusterService;
import se.kth.karamel.backend.Experiment;
import se.kth.karamel.backend.command.CommandResponse;
import se.kth.karamel.backend.command.CommandService;
import se.kth.karamel.backend.github.Github;
import se.kth.karamel.backend.github.GithubUser;
import se.kth.karamel.backend.github.OrgItem;
import se.kth.karamel.backend.github.RepoItem;
import se.kth.karamel.backend.github.util.ChefExperimentExtractor;
import se.kth.karamel.backend.launcher.amazon.Ec2Context;
import se.kth.karamel.backend.launcher.amazon.Ec2Launcher;
import se.kth.karamel.backend.running.model.ClusterRuntime;
import se.kth.karamel.backend.running.model.GroupRuntime;
import se.kth.karamel.backend.running.model.MachineRuntime;
import se.kth.karamel.backend.running.model.serializers.ClusterEntitySerializer;
import se.kth.karamel.backend.running.model.serializers.GroupEntitySerializer;
import se.kth.karamel.backend.running.model.serializers.MachineEntitySerializer;
import se.kth.karamel.backend.running.model.serializers.ShellCommandSerializer;
import se.kth.karamel.backend.running.model.serializers.DefaultTaskSerializer;
import se.kth.karamel.backend.running.model.tasks.AptGetEssentialsTask;
import se.kth.karamel.backend.running.model.tasks.InstallBerkshelfTask;
import se.kth.karamel.backend.running.model.tasks.MakeSoloRbTask;
import se.kth.karamel.backend.running.model.tasks.RunRecipeTask;
import se.kth.karamel.backend.running.model.tasks.ShellCommand;
import se.kth.karamel.backend.running.model.tasks.VendorCookbookTask;
import se.kth.karamel.common.exception.KaramelException;
import se.kth.karamel.cookbook.metadata.KaramelizedCookbook;
import se.kth.karamel.common.Confs;
import se.kth.karamel.common.Ec2Credentials;
import se.kth.karamel.common.Settings;
import se.kth.karamel.common.SshKeyPair;
import se.kth.karamel.common.SshKeyService;
import se.kth.karamel.cookbook.metadata.Berksfile;
import se.kth.karamel.cookbook.metadata.DefaultRb;
import se.kth.karamel.cookbook.metadata.ExperimentRecipe;
import se.kth.karamel.cookbook.metadata.InstallRecipe;
import se.kth.karamel.cookbook.metadata.KaramelFile;
import se.kth.karamel.cookbook.metadata.MetadataRb;

/**
 * Implementation of the Karamel Api for UI
 *
 * @author kamal
 */
public class KaramelApiImpl implements KaramelApi {

  private static final ClusterService clusterService = ClusterService.getInstance();

  @Override
  public String commandCheatSheet() throws KaramelException {
    return CommandService.processCommand("help").getResult();
  }

  @Override
  public CommandResponse processCommand(String command, String... args) throws KaramelException {
    return CommandService.processCommand(command, args);
  }

  @Override
  public String getCookbookDetails(String cookbookUrl, boolean refresh) throws KaramelException {
    if (refresh) {
      KaramelizedCookbook cb = CookbookCache.load(cookbookUrl);
      return cb.getMetadataJson();
    } else {
      KaramelizedCookbook cb = CookbookCache.get(cookbookUrl);
      return cb.getMetadataJson();
    }
  }

  @Override
  public String jsonToYaml(String json) throws KaramelException {
    return ClusterDefinitionService.jsonToYaml(json);
  }

  @Override
  public String yamlToJson(String yaml) throws KaramelException {
    return ClusterDefinitionService.yamlToJson(yaml);
  }

  @Override
  public Ec2Credentials loadEc2CredentialsIfExist() throws KaramelException {
    Confs confs = Confs.loadKaramelConfs();
    return Ec2Launcher.readCredentials(confs);
  }

  @Override
  public boolean updateEc2CredentialsIfValid(Ec2Credentials credentials) throws KaramelException {
    Ec2Context context = Ec2Launcher.validateCredentials(credentials);
    Confs confs = Confs.loadKaramelConfs();
    confs.put(Settings.AWS_ACCESS_KEY, credentials.getAccessKey());
    confs.put(Settings.AWS_SECRET_KEY, credentials.getSecretKey());
    confs.writeKaramelConfs();
    clusterService.registerEc2Context(context);
    return true;
  }

  @Override
  public String getClusterStatus(String clusterName) throws KaramelException {
    ClusterRuntime clusterManager = clusterService.clusterStatus(clusterName);
    Gson gson = new GsonBuilder().
        registerTypeAdapter(ClusterRuntime.class, new ClusterEntitySerializer()).
        registerTypeAdapter(MachineRuntime.class, new MachineEntitySerializer()).
        registerTypeAdapter(GroupRuntime.class, new GroupEntitySerializer()).
        registerTypeAdapter(ShellCommand.class, new ShellCommandSerializer()).
        registerTypeAdapter(RunRecipeTask.class, new DefaultTaskSerializer()).
        registerTypeAdapter(MakeSoloRbTask.class, new DefaultTaskSerializer()).
        registerTypeAdapter(VendorCookbookTask.class, new DefaultTaskSerializer()).
        registerTypeAdapter(AptGetEssentialsTask.class, new DefaultTaskSerializer()).
        registerTypeAdapter(InstallBerkshelfTask.class, new DefaultTaskSerializer()).
        setPrettyPrinting().
        create();
    String json = gson.toJson(clusterManager);
    return json;
  }

  @Override
  public void pauseCluster(String clusterName) throws KaramelException {
    clusterService.pauseCluster(clusterName);
  }

  @Override
  public void resumeCluster(String clusterName) throws KaramelException {
    clusterService.resumeCluster(clusterName);
  }

  @Override
  public void purgeCluster(String clusterName) throws KaramelException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void startCluster(String json) throws KaramelException {
    clusterService.startCluster(json);
  }

  @Override
  public String getInstallationDag(String clusterName) throws KaramelException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public SshKeyPair loadSshKeysIfExist() throws KaramelException {
    Confs confs = Confs.loadKaramelConfs();
    SshKeyPair sshKeys = SshKeyService.loadSshKeys(confs);
    return sshKeys;
  }

  @Override
  public SshKeyPair loadSshKeysIfExist(String clusterName) throws KaramelException {
    Confs confs = Confs.loadAllConfsForCluster(clusterName);
    SshKeyPair sshKeys = SshKeyService.loadSshKeys(confs);
    return sshKeys;
  }

  @Override
  public SshKeyPair generateSshKeysAndUpdateConf() throws KaramelException {
    SshKeyPair sshkeys = SshKeyService.generateAndStoreSshKeys();
    Confs confs = Confs.loadKaramelConfs();
    confs.put(Settings.SSH_PRIVKEY_PATH_KEY, sshkeys.getPrivateKeyPath());
    confs.put(Settings.SSH_PUBKEY_PATH_KEY, sshkeys.getPublicKeyPath());
    confs.writeKaramelConfs();
    return sshkeys;
  }

  @Override
  public SshKeyPair generateSshKeysAndUpdateConf(String clusterName) throws KaramelException {
    SshKeyPair sshkeys = SshKeyService.generateAndStoreSshKeys(clusterName);
    Confs confs = Confs.loadJustClusterConfs(clusterName);
    confs.put(Settings.SSH_PRIVKEY_PATH_KEY, sshkeys.getPrivateKeyPath());
    confs.put(Settings.SSH_PUBKEY_PATH_KEY, sshkeys.getPublicKeyPath());
    confs.writeClusterConfs(clusterName);
    return sshkeys;
  }

  @Override
  public SshKeyPair registerSshKeys(SshKeyPair keypair) throws KaramelException {
    Confs confs = Confs.loadKaramelConfs();
    saveSshConfs(keypair, confs);
    confs.writeKaramelConfs();
//    keypair = SshKeyService.loadSshKeys(confs);
    keypair = SshKeyService.loadSshKeys(keypair.getPublicKeyPath(),
        keypair.getPrivateKeyPath(), keypair.getPassphrase());
    clusterService.registerSshKeyPair(keypair);
    return keypair;
  }

  private void saveSshConfs(SshKeyPair keypair, Confs confs) {
    confs.put(Settings.SSH_PRIVKEY_PATH_KEY, keypair.getPrivateKeyPath());
    confs.put(Settings.SSH_PUBKEY_PATH_KEY, keypair.getPublicKeyPath());
    if (keypair.getPassphrase() != null && keypair.getPassphrase().isEmpty() == false) {
      confs.put(Settings.SSH_PRIVKEY_PASSPHRASE, keypair.getPassphrase());
    }
  }

  @Override
  public SshKeyPair registerSshKeys(String clusterName, SshKeyPair keypair) throws KaramelException {
    Confs confs = Confs.loadJustClusterConfs(clusterName);
    saveSshConfs(keypair, confs);
    confs.writeClusterConfs(clusterName);
    keypair = SshKeyService.loadSshKeys(keypair.getPublicKeyPath(), keypair.getPrivateKeyPath(),
        keypair.getPassphrase());
    clusterService.registerSshKeyPair(clusterName, keypair);
    return keypair;
  }

  @Override
  public void registerSudoPassword(String password) {
    ClusterService.getInstance().getCommonContext().setSudoAccountPassword(password);
  }

  @Override
  public List<OrgItem> listGithubOrganizations() throws KaramelException {
    return Github.getOrganizations();
  }

  @Override
  public List<RepoItem> listGithubRepos(String organization) throws KaramelException {
    return Github.getRepos(organization);
  }

  @Override
  public GithubUser registerGithubAccount(String user, String password) throws KaramelException {
    return Github.registerCredentials(user, password);
  }

  @Override
  public GithubUser loadGithubCredentials() throws KaramelException {
    return Github.loadGithubCredentials();
  }

  private void initGithubRepo(String owner, String repo, String description) throws KaramelException {
    if (owner == null || owner.isEmpty()) {
      Github.createRepoForUser(repo, description);
    } else {
      Github.createRepoForOrg(owner, repo, description);
    }
  }

  @Override
  public void commitAndPushExperiment(String owner, String repoName, Experiment experiment)
      throws KaramelException {

    // Create the repo if it doesn't exist and clone it to a local directory
    File f = Github.getRepoDirectory(repoName);
    if (f.exists() == false) {
      initGithubRepo(owner, repoName, experiment.getDescription());
      // Scaffold a new experiment project with Karamel/Chef
      Github.scaffoldRepo(repoName);
    }

    // For all config and script files, compile them and generate Karamel/Chef files
    ChefExperimentExtractor.parseAttributesAddToGit(owner, repoName, experiment);

    ChefExperimentExtractor.parseRecipesAddToGit(owner, repoName, experiment);

    // Commit and push all changes to github
    Github.commitPush(owner, repoName);

  }

  @Override
  public Experiment loadExperiment(String githubRepoUrl) throws KaramelException {
    Experiment ec = new Experiment();

    int e = githubRepoUrl.lastIndexOf(".git");
    int s = githubRepoUrl.lastIndexOf("/");
    if (s == -1 || e == -1) {
      throw new KaramelException("Misformed url: " + githubRepoUrl);
    }
    int s1 = githubRepoUrl.lastIndexOf("/", s - 1);
    if (s1 == -1) {
      throw new KaramelException("Misformed url: " + githubRepoUrl);
    }

    String repoName = githubRepoUrl.substring(s, e);
    ec.setGithubRepo(repoName);
    String owner = githubRepoUrl.substring(s1, s);
    ec.setGithubOwner(owner);
    if (repoName == null || owner == null || repoName.isEmpty() || owner.isEmpty()) {
      throw new KaramelException("Misformed url repo/owner: " + githubRepoUrl);
    }

    Github.cloneRepo(owner, repoName);

    KaramelizedCookbook kc = new KaramelizedCookbook(githubRepoUrl);
    MetadataRb metadata = kc.getMetadataRb();
    KaramelFile kf = kc.getKaramelFile();
    String metadataJson = kc.getMetadataJson();
//    String configFile = kc.getConfigFile();
    Berksfile bf = kc.getBerksFile();
    DefaultRb attributes = kc.getDefaultRb();
    List<ExperimentRecipe> er = kc.getExperimentRecipes();
    InstallRecipe ir = kc.getInstallRecipe();

    ec.setUser(attributes.getValue(repoName + "/user"));
    ec.setGroup(attributes.getValue(repoName + "/group"));
    ec.setUrlBinary(attributes.getValue(repoName + "/url"));
    ec.setExperimentSetupCode(ir.getSetupCode());
    for (ExperimentRecipe r : er) {
      Experiment.Code exp = new Experiment.Code(r.getRecipeName(), r.getScriptContents(), r.getConfigFileName(),
          r.getConfigFileContents(), r.getScriptType());
      ec.addExperiment(exp);
    }
    return ec;
  }

  @Override
  public RepoItem createGithubRepo(String org, String repo, String description) throws KaramelException {
    return Github.createRepoForOrg(org, repo, description);
  }

  @Override
  public void removeFileFromExperiment(String owner, String repo, String experimentName) {
    try {
      Github.removeFile(owner, repo, experimentName);
    } catch (KaramelException ex) {
      // Do nothing - Repository hasn't been created yet. That's ok.");
    }
  }

  @Override
  public void removeRepo(String owner, String repo) {
    
  }

}
