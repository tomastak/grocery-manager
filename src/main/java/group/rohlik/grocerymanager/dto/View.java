package group.rohlik.grocerymanager.dto;

/**
 *
 * @author Tomas Kramec
 */
public class View {

    public interface All {}

    public interface Update extends All {}
    public interface Create extends Update {}
    public interface Read extends Create {}
}
