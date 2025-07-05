package com.example.fabric;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.CommitException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

@Service
public class FabricService {
    private static final Path PATH_TO_TEST_NETWORK = Paths.get("/home/farshad/go/src/github.com/farshadmrd/fabric-samples/test-network");
    private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
    private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");
    private static final String PEER_ENDPOINT = "localhost:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private Gateway gateway;
    private Contract contract;
    private ManagedChannel channel;

    public FabricService() throws Exception {
        initializeConnection();
    }

    private void initializeConnection() throws Exception {
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                .trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                        "organizations/peerOrganizations/org1.example.com/" +
                                "peers/peer0.org1.example.com/tls/ca.crt"))
                        .toFile())
                .build();

        channel = Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
                .overrideAuthority(OVERRIDE_AUTH)
                .build();

        Gateway.Builder builder = Gateway.newInstance()
                .identity(new X509Identity("Org1MSP",
                        Identities.readX509Certificate(
                                Files.newBufferedReader(
                                        PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                                "organizations/peerOrganizations/org1.example.com/" +
                                                        "users/User1@org1.example.com/msp/signcerts/cert.pem"))))))
                .signer(
                        Signers.newPrivateKeySigner(
                                Identities.readPrivateKey(
                                        Files.newBufferedReader(
                                                Files.list(PATH_TO_TEST_NETWORK.resolve(
                                                        Paths.get(
                                                                "organizations/peerOrganizations/org1.example.com/"
                                                                        +
                                                                        "users/User1@org1.example.com/msp/keystore")))
                                                        .findFirst().orElseThrow()))))
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        gateway = builder.connect();
        contract = gateway.getNetwork(CHANNEL_NAME).getContract(CHAINCODE_NAME);
    }

    public String createBasil(String id, String country) throws GatewayException, CommitException {
        byte[] result = contract.submitTransaction("createBasil", id, country);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String readBasil(String id) throws GatewayException {
        try {
            byte[] result = contract.evaluateTransaction("readBasil", id);
            if (result == null || result.length == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No basil found with ID: " + id);
            }
            return new String(result, StandardCharsets.UTF_8);
        } catch (GatewayException e) {
            if (e.getMessage().contains("No basil found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No basil found with ID: " + id);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting basil: " + e.getMessage());
        }
    }

    

    public String deleteBasil(String id) throws GatewayException, CommitException {
        // First, check who owns the basil
        String basilData = readBasil(id);
        
        // Parse the JSON to check ownership (simple string check)
        if (basilData.contains("\"orgId\":\"Org2MSP\"")) {
            // For now, we'll create a connection as Org2MSP to delete the basil
            return deleteBasilAsOrg2(id);
        }
        
        // If owned by Org1MSP, use the existing connection
        contract.submitTransaction("deleteBasil", id);
        return "Basil deleted successfully";
    }
    
    private String deleteBasilAsOrg2(String id) throws GatewayException, CommitException {
        Gateway org2Gateway = null;
        ManagedChannel org2Channel = null;
        
        try {
            // Create Org2MSP connection
            ChannelCredentials org2Credentials = TlsChannelCredentials.newBuilder()
                    .trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                            "organizations/peerOrganizations/org2.example.com/" +
                                    "peers/peer0.org2.example.com/tls/ca.crt"))
                            .toFile())
                    .build();

            org2Channel = Grpc.newChannelBuilder("localhost:9051", org2Credentials)
                    .overrideAuthority("peer0.org2.example.com")
                    .build();

            Gateway.Builder org2Builder = Gateway.newInstance()
                    .identity(new X509Identity("Org2MSP",
                            Identities.readX509Certificate(
                                    Files.newBufferedReader(
                                            PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                                    "organizations/peerOrganizations/org2.example.com/" +
                                                            "users/User1@org2.example.com/msp/signcerts/cert.pem"))))))
                    .signer(
                            Signers.newPrivateKeySigner(
                                    Identities.readPrivateKey(
                                            Files.newBufferedReader(
                                                    Files.list(PATH_TO_TEST_NETWORK.resolve(
                                                            Paths.get(
                                                                    "organizations/peerOrganizations/org2.example.com/"
                                                                            +
                                                                            "users/User1@org2.example.com/msp/keystore")))
                                                            .findFirst().orElseThrow()))))
                    .connection(org2Channel)
                    .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                    .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

            org2Gateway = org2Builder.connect();
            Contract org2Contract = org2Gateway.getNetwork(CHANNEL_NAME).getContract(CHAINCODE_NAME);
            
            // Perform the delete as Org2MSP
            org2Contract.submitTransaction("deleteBasil", id);
            return "Basil deleted successfully by Org2MSP";
            
        } catch (Exception e) {
            throw new RuntimeException("Error deleting basil as Org2MSP: " + e.getMessage(), e);
        } finally {
            // Cleanup
            if (org2Gateway != null) {
                org2Gateway.close();
            }
            if (org2Channel != null) {
                org2Channel.shutdownNow();
            }
        }
    }

    public String updateBasilState(String id, String gps, Long timestamp, String temp, String humidity, String status) 
            throws GatewayException, CommitException {
        contract.submitTransaction("updateBasilState", id, gps, timestamp.toString(), temp, humidity, status);
        return "Basil state updated successfully";
    }

    public String getBasilHistory(String id) throws GatewayException {
        byte[] result = contract.evaluateTransaction("getHistory", id);
        return new String(result, StandardCharsets.UTF_8);
    }

    public String transferBasilOwnership(String id, String newOrgId, String newName) 
            throws GatewayException, CommitException {
        contract.submitTransaction("transferOwnership", id, newOrgId, newName);
        return "Basil ownership transferred successfully";
    }

    public void cleanup() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }
} 