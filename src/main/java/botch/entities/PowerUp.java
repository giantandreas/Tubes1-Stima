package botch.entities;

import com.google.gson.annotations.SerializedName;
import botch.enums.PowerUpType;

public class PowerUp {
    @SerializedName("type")
    public PowerUpType type;

    @SerializedName("value")
    public int value;
}
