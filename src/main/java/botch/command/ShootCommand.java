package botch.command;

import botch.enums.Direction;
import botch.entities.Position;

public class ShootCommand implements Command {

    private Direction direction;

    public ShootCommand(Direction direction) {
        this.direction = direction;
    }

    @Override
    public String render() {
        return String.format("shoot %s", direction.name());
    }
}
