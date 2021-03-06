package botch.entities;

import com.google.gson.annotations.SerializedName;

public class Opponent {
    @SerializedName("id")
    public int id;

    @SerializedName("score")
    public int score;

    @SerializedName("currentWormId")
    public int currentWormId;

    @SerializedName("worms")
    public Worm[] worms;
}
