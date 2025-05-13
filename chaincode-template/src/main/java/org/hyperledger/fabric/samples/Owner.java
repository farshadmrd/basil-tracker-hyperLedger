package org.hyperledger.fabric.samples;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import com.owlike.genson.annotation.JsonProperty;
import java.util.Objects;

@DataType()
public final class Owner {

    @Property()
    private String orgId;

    @Property()
    private String user;
// e.g., Greenhouse, Transporter, Supermarket

    public Owner(@JsonProperty("orgId") final String orgId,
            @JsonProperty("user") final String user) {
        this.orgId = orgId;
        this.user = user;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getUser() {
        return user;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Owner))
            return false;
        Owner owner = (Owner) o;
        return Objects.equals(orgId, owner.orgId)
                && Objects.equals(user, owner.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgId, user);
    }

    @Override
    public String toString() {
        return "Owner{" + "orgId='" + orgId + '\'' + ", user='" + user + '\'' + '}';
    }
}
