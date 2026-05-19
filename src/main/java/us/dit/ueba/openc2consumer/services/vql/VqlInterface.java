package us.dit.ueba.openc2consumer.services.vql;

public interface VqlInterface {

    public static enum EvidenceType {
        USERLOGON,
        USERSESSION,
        USERLOGOUT,
        USERSESSIONDURATION
    }

    public void sendNewArtefact(EvidenceType evidenceType);

    public void startMonitoring(EvidenceType evidenceType);

    public void addUser(EvidenceType evidenceType, String username);

    public void deleteUser(EvidenceType evidenceType, String username);

}
