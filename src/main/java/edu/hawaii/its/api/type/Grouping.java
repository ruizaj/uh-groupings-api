package edu.hawaii.its.api.type;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "grouping")
public class Grouping {

    @Id
    @Column(name = "path")
    private String path;

    @Column
    private String name;

    @Column
    private String description;

    @OneToOne
    private Group basis;

    @OneToOne
    private Group exclude;

    @OneToOne
    private Group include;

    @OneToOne
    private Group composite;

    @OneToOne
    private Group owners;

    @OneToMany
    @LazyCollection(LazyCollectionOption.FALSE)
    @NotFound(action = NotFoundAction.IGNORE)
    private List<SyncDestination> syncDestinations = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, Boolean> syncDestinationsState = new HashMap<>();

    @Column
    private boolean isOptInOn = false;

    @Column
    private boolean isOptOutOn = false;

    // Constructor.
    public Grouping() {
        this("");
    }

    // Constructor.
    public Grouping(String path) {
        setPath(path);

        setDescription("");
        setBasis(new EmptyGroup());
        setExclude(new EmptyGroup());
        setInclude(new EmptyGroup());
        setComposite(new EmptyGroup());
        setOwners(new EmptyGroup());
    }

    public List<SyncDestination> getSyncDestinations() {
        return syncDestinations;
    }

    public void setSyncDestinations(List<SyncDestination> syncDestinations) {
        this.syncDestinations = syncDestinations;

        for (SyncDestination destination : syncDestinations) {

            syncDestinationsState.put(destination.getName(), destination.isSynced());
        }
    }

    public boolean isSyncDestinationOn(String key) {

        return syncDestinationsState.get(key);
    }

    public void changeSyncDestinationState(String key, Boolean boo) {

        syncDestinationsState.replace(key, boo);

        for (SyncDestination destination : syncDestinations) {
            if (destination.getName().equals(key)) {
                destination.setSynced(boo);
            }
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
        this.name = this.path;
        int index = this.name.lastIndexOf(':');
        if (index != -1) {
            this.name = this.name.substring(index + 1);
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public Group getBasis() {
        return basis;
    }

    public void setBasis(Group basis) {
        this.basis = basis != null ? basis : new EmptyGroup();
    }

    public Group getExclude() {
        return exclude;
    }

    public void setExclude(Group exclude) {
        this.exclude = exclude != null ? exclude : new EmptyGroup();
    }

    public Group getInclude() {
        return include;
    }

    public void setInclude(Group include) {
        this.include = include != null ? include : new EmptyGroup();
    }

    public Group getComposite() {
        return composite;
    }

    public void setComposite(Group composite) {
        this.composite = composite != null ? composite : new EmptyGroup();
    }

    public Group getOwners() {
        return owners;
    }

    public void setOwners(Group owners) {
        this.owners = owners != null ? owners : new EmptyGroup();
    }

    public boolean isOptInOn() {
        return isOptInOn;
    }

    public void setOptInOn(boolean isOptInOn) {
        this.isOptInOn = isOptInOn;
    }

    public boolean isOptOutOn() {
        return isOptOutOn;
    }

    public void setOptOutOn(boolean isOptOutOn) {
        this.isOptOutOn = isOptOutOn;
    }

    @Override
    public String toString() {
        return "Grouping [name=" + name
                + ", path=" + path
                + ", OptInOn=" + isOptInOn()
                + ", OptOutOn=" + isOptOutOn()
                + ", basis=" + basis
                + ", owners=" + owners
                + "]";
    }
}
