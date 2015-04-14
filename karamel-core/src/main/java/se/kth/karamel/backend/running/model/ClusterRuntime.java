/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.karamel.backend.running.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.kth.karamel.client.model.json.JsonCluster;
import se.kth.karamel.client.model.json.JsonGroup;

/**
 *
 * @author kamal
 */
public class ClusterRuntime {

  public static enum ClusterPhases {
    NOT_STARTED, PRECLEANING, PRECLEANED, FORKING_GROUPS, GROUPS_FORKED, FORKING_MACHINES, MACHINES_FORKED, INSTALLING, INSTALLED, PURGING ;
  }
  
  private final String name;
  
  private ClusterPhases phase = ClusterPhases.NOT_STARTED;
  
  private boolean failed = false;
  
  private boolean paused = false;

  private List<GroupRuntime> groups = new ArrayList<>();
  
  private Map<String, Failure> failures = new HashMap<>();
  
  public ClusterRuntime(String name) {
    this.name = name;
  }

  public ClusterRuntime(JsonCluster definition) {
    this.name = definition.getName();
    List<JsonGroup> definedGroups = definition.getGroups();
    for (JsonGroup jg : definedGroups) {
      GroupRuntime group = new GroupRuntime(this, jg);
      groups.add(group);
    }
  }

  public String getName() {
    return name;
  }
  
  public void setGroups(List<GroupRuntime> groups) {
    this.groups = groups;
  }

  public List<GroupRuntime> getGroups() {
    return groups;
  }

  public ClusterPhases getPhase() {
    return phase;
  }

  public void setPhase(ClusterPhases phase) {
    this.phase = phase;
  }

  public boolean isFailed() {
    return failed;
  }

  public void issueFailure(Failure failure) {
    this.failed = true;
    failures.put(failure.hash(), failure);
  }
  
  public void resolveFailure(String hash) {
    failures.remove(hash);
    failed = !failures.isEmpty();
  }
  
  public void resolveFailures() {
    failures.clear();
    failed = false;
  }

  public Map<String, Failure> getFailures() {
    return failures;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

}