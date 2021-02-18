package botch.command;

import botch.entities.Position;

public class SnowballCommand implements Command{
    private Position position;

    public SnowballCommand(Position position){this.position=position; }

    @Override
    public String render(){return String.format("snowball %d %d", this.position.x, this.position.y);}
}
