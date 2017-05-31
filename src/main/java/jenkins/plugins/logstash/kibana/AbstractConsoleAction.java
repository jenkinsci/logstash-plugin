/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jenkins.plugins.logstash.kibana;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.plugins.logstash.util.UniqueIdHelper;

/**
 * Wrapper base, which is required to nest {@code buildCaption.jelly}.
 * @author Oleg Nenashev
 */
public abstract class AbstractConsoleAction implements Action {
    private final String jobId;
    private final Run run;
    
    public AbstractConsoleAction(Run run) {
        this.run = run;
        this.jobId =  UniqueIdHelper.getOrCreateId(run);
    }
    
    @Override
    public String getDisplayName() {
        return "External log (" + getDataSourceDisplayName() + ")";
    }
    
    public Run getRun() {
        return run;
    }

    public String getJobId() {
        return jobId;
    }
    
    public boolean isLogUpdated() {
        return run.isLogUpdated();
    }
    
    public abstract String getDataSourceDisplayName();
}
