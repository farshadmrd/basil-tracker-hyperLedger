package org.hyperledger.fabric.samples;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import com.owlike.genson.Genson;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Contract(
        name = "BasilContract",
        info = @Info(
                title = "My Smart contract",
                description = "The hyperlegendary asset transfer",
                version = "1.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public class BasilContract implements ContractInterface {

    private final Genson genson = new Genson();
    private static final String SUPERMARKET_ORG = "Org2MSP";

    // Create a new basil plant
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createBasil(Context ctx, String qrCode, String origin) {
        rejectIfSupermarket(ctx);
        ChaincodeStub stub = ctx.getStub();

        if (stub.getStringState(qrCode) != null && !stub.getStringState(qrCode).isEmpty()) {
            throw new ChaincodeException("Basil already exists with QR: " + qrCode);
        }

        String orgId = ctx.getClientIdentity().getMSPID();
        Owner owner = new Owner(orgId, "Greenhouse");
        Long creationTimestamp = stub.getTxTimestamp().getEpochSecond();

        BasilLeg initialLeg = createBasilLeg(creationTimestamp, origin, "N/A", "N/A", owner);
        List<BasilLeg> history = new ArrayList<>();
        history.add(initialLeg);

        Basil basil = new Basil(qrCode, creationTimestamp, origin, "Created", origin, owner, history);

        stub.putStringState(qrCode, genson.serialize(basil));
    }


    // Stop tracking a basil plant (delete it)
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteBasil(Context ctx, String qrCode) {
        ChaincodeStub stub = ctx.getStub();
        Basil basil = readBasil(ctx, qrCode);

        String clientOrg = getClientOrgId(ctx);
        if (!basil.getCurrentOwner().getOrgId().equals(clientOrg)) {
            throw new ChaincodeException("Only the current owner can delete the plant.");
        }

        stub.delState(qrCode);
    }

    // Update state: add a BasilLeg and change current info
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateBasilState(Context ctx, String qrCode, String gps, Long timestamp, String temp, String humidity,
            String status) {
        rejectIfSupermarket(ctx);
        ChaincodeStub stub = ctx.getStub();
        Basil basil = readBasil(ctx, qrCode);

        String clientOrg = getClientOrgId(ctx);
        if (!basil.getCurrentOwner().getOrgId().equals(clientOrg)) {
            throw new ChaincodeException("Only the current owner can update the plant state.");
        }

        Owner owner = basil.getCurrentOwner();
        BasilLeg leg = createBasilLeg(timestamp, gps, temp, humidity, owner);
        basil.getTransportHistory().add(leg);

        Basil updated = new Basil(
                basil.getQrCode(),
                basil.getCreationTimestamp(),
                basil.getOrigin(),
                status,
                gps,
                owner,
                basil.getTransportHistory());

        stub.putStringState(qrCode, genson.serialize(updated));
    }

    // Get current state of a basil plant
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Basil readBasil(Context ctx, String qrCode) {
        String data = ctx.getStub().getStringState(qrCode);
        if (data == null || data.isEmpty()) {
            throw new ChaincodeException("No basil found with QR: " + qrCode);
        }
        return genson.deserialize(data, Basil.class);
    }


    // Get the transport history
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<BasilLeg> getHistory(Context ctx, String qrCode) {
        Basil basil = readBasil(ctx, qrCode);
        return basil.getTransportHistory();
    }

    // Transfer ownership to another organization
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void transferOwnership(Context ctx, String qrCode, String newOrgId, String newName) {
        rejectIfSupermarket(ctx);
        ChaincodeStub stub = ctx.getStub();
        Basil basil = readBasil(ctx, qrCode);

        String clientOrg = getClientOrgId(ctx);
        if (!basil.getCurrentOwner().getOrgId().equals(clientOrg)) {
            throw new ChaincodeException("Only the current owner can transfer ownership.");
        }

        Owner newOwner = new Owner(newOrgId, newName);
        Basil updated = new Basil(
                basil.getQrCode(),
                basil.getCreationTimestamp(),
                basil.getOrigin(),
                basil.getCurrentStatus(),
                basil.getCurrentGps(),
                newOwner,
                basil.getTransportHistory());

        stub.putStringState(qrCode, genson.serialize(updated));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getBasilHistory(final Context ctx, final String id) {
        ChaincodeStub stub = ctx.getStub();
        String key = id;  // Use the ID directly as the key

        // Check if basil exists
        String data = stub.getStringState(key);
        if (data == null || data.isEmpty()) {
            throw new ChaincodeException("Basil with ID " + id + " does not exist");
        }

        try {
            // Get history iterator
            QueryResultsIterator<KeyModification> historyIterator = stub.getHistoryForKey(key);
            
            // Convert history to JSON array
            List<Map<String, Object>> history = new ArrayList<>();
            for (KeyModification modification : historyIterator) {
                try {
                    // Parse the value using Genson
                    Map<String, Object> record = genson.deserialize(modification.getStringValue(), Map.class);
                    // Add transaction timestamp
                    record.put("timestamp", modification.getTimestamp().getEpochSecond());
                    history.add(record);
                } catch (Exception e) {
                    // Skip invalid records
                    continue;
                }
            }

            // Return history as JSON array using Genson
            return genson.serialize(history);
        } catch (Exception e) {
            throw new ChaincodeException("Error getting basil history: " + e.getMessage());
        }
    }

    private String getClientOrgId(Context ctx) {
        return ctx.getClientIdentity().getMSPID();
    }

    private void rejectIfSupermarket(Context ctx) {
        if (SUPERMARKET_ORG.equals(ctx.getClientIdentity().getMSPID())) {
            throw new ChaincodeException("Operation not permitted for the supermarket organization.");
        }
    }

    private BasilLeg createBasilLeg(Long timestamp, String gps, String temperature, String humidity, Owner owner) {
        return new BasilLeg(timestamp, gps, temperature, humidity, owner);
    }
}
