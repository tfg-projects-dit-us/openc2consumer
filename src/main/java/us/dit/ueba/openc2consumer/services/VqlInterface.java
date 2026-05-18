package us.dit.ueba.openc2consumer.services;

public interface VqlInterface {

    public static enum EvidenceType {
        USERLOGON,
        USERSESSION,
        USERLOGOUT,
        USERSESSIONDURATION
    }

    public void sendNewArtefact(EvidenceType evidenceType);

    public void startMonitoring(EvidenceType evidenceType);

}
