package us.dit.ueba.openc2consumer.actuators;

import org.oasis.openc2.lycan.OpenC2Message;
import org.oasis.openc2.lycan.OpenC2Response;

public abstract class ActuatorCommons implements Actuator {
    // Common functionality for all actuators
    // This class can contain common methods and fields that are shared by all actuator implementations.
    protected final String profileName;


    public ActuatorCommons(String profileName) {
        this.profileName = profileName;
    }
    @Override
    public String getProfileName() {
        return profileName;
    }
    @Override
    abstract public OpenC2Response solve(OpenC2Message message);
    @Override
    abstract public boolean supports(OpenC2Message message);
   
}
