package org.hyperledger.fabric.samples;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import com.owlike.genson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

@DataType()
public final class Basil {

    @Property()
    private final String qrCode;

    @Property()
    private final Long creationTimestamp; 

    @Property()
    private final String origin; // Greenhouse or starting point

    @Property()
    private final String currentStatus; // e.g., In Transit, Delivered

    @Property()
    private final String currentGps;

    @Property()
    private final Owner currentOwner;

    @Property()
    private final List<BasilLeg> transportHistory;

    public Basil(
            @JsonProperty("qrCode") String qrCode,
            @JsonProperty("creationTimestamp") Long creationTimestamp,
            @JsonProperty("origin") String origin,
            @JsonProperty("currentStatus") String currentStatus,
            @JsonProperty("currentGps") String currentGps,
            @JsonProperty("currentOwner") Owner currentOwner,
            @JsonProperty("transportHistory") List<BasilLeg> transportHistory) {
        this.qrCode = qrCode;
        this.creationTimestamp = creationTimestamp;
        this.origin = origin;
        this.currentStatus = currentStatus;
        this.currentGps = currentGps;
        this.currentOwner = currentOwner;
        this.transportHistory = transportHistory;
    }

    public String getQrCode() {
        return qrCode;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public String getOrigin() {
        return origin;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getCurrentGps() {
        return currentGps;
    }

    public Owner getCurrentOwner() {
        return currentOwner;
    }

    public List<BasilLeg> getTransportHistory() {
        return transportHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Basil)) return false;
        Basil basil = (Basil) o;
        return Objects.equals(qrCode, basil.qrCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qrCode);
    }

    @Override
    public String toString() {
        return "Basil{" +
                "qrCode='" + qrCode + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", origin='" + origin + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", currentGps='" + currentGps + '\'' +
                ", currentOwner=" + currentOwner +
                ", transportHistory=" + transportHistory +
                '}';
    }
}