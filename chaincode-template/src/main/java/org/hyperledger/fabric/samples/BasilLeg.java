package org.hyperledger.fabric.samples;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import com.owlike.genson.annotation.JsonProperty;

import java.util.Objects;

@DataType()
public final class BasilLeg {

    @Property()
    private final Long timestamp;

    @Property()
    private final String gps;

    @Property()
    private final String temperature;

    @Property()
    private final String humidity;

    @Property()
    private final Owner owner;

    public BasilLeg(
            @JsonProperty("timestamp") Long timestamp,
            @JsonProperty("gps") String gps,
            @JsonProperty("temperature") String temperature,
            @JsonProperty("humidity") String humidity,
            @JsonProperty("owner") Owner owner) {
        this.timestamp = timestamp;
        this.gps = gps;
        this.temperature = temperature;
        this.humidity = humidity;
        this.owner = owner;
    }

    public Long getTimestamp() { 
        return timestamp;
    }

    public String getGps() {
        return gps;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public Owner getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasilLeg)) return false;
        BasilLeg leg = (BasilLeg) o;
        return Objects.equals(timestamp, leg.timestamp) &&
               Objects.equals(gps, leg.gps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, gps);
    }

    @Override
    public String toString() {
        return "BasilLeg{" +
                "timestamp=" + timestamp + 
                ", gps='" + gps + '\'' +
                ", temperature='" + temperature + '\'' +
                ", humidity='" + humidity + '\'' +
                ", owner=" + owner +
                '}';
    }
}
