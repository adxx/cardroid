package de.jlab.cardroid.devices;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;
import de.jlab.cardroid.utils.MultiMap;

/* TODO add bluetooth support
 * - create a device wrapper to unify usb and bluetooth devices
 * - create a devicehandler wrapper to unify usb and bluetooth handling
 * - create a serial connection wrapper to unify usb and bluetooth serial connection
 */
public abstract class Device {

    public enum State {
        ATTACHED,
        OPEN,
        READY,
        INVALID
    }

    private ArrayList<Feature> features = new ArrayList<>();
    private ArrayList<StateObserver> stateObservers = new ArrayList<>();
    //private ArrayList<FeatureObserver<? extends Feature>> featureObservers = new ArrayList<>();

    private MultiMap<Class<? extends Feature>, FeatureChangeObserver> featureObservers = new MultiMap<>();

    private Application app;
    private DeviceEntity descriptor;
    private DeviceConnectionId connectionId;
    private State state = State.ATTACHED;

    public Device(@NonNull Application app) {
        this.app = app;
    }

    //////////////////////////////////////////
    // External methods for app interaction //
    //////////////////////////////////////////

    /* TODO: Remove this, if it turns out to be obsolete
    @Nullable
    public <FeatureType extends Feature> FeatureType getFeature(Class<FeatureType> featureType) {
        for (int i = 0; i < this.features.size(); i++) {
            Feature feature = this.features.get(i);
            if (featureType.isInstance(feature)) {
                return featureType.cast(feature);
            }
        }
        return null;
    }
    */

    public void addStateObserver(@NonNull StateObserver observer) {
        this.stateObservers.add(observer);

        // Notify new observer about the current state
        observer.onStateChange(this, this.state, this.state);
    }

    public void removeStateObserver(@NonNull StateObserver observer) {
        this.stateObservers.remove(observer);
    }

    public <FT extends Feature> void addFeatureObserver(@NonNull FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass) {
        this.featureObservers.put(featureClass, observer);

        for (int i = 0; i < this.features.size(); i++) {
            Feature feature = this.features.get(i);
            if (featureClass.isAssignableFrom(feature.getClass())) {
                observer.onFeatureChange(Objects.requireNonNull(featureClass.cast(feature)), Feature.State.AVAILABLE);
            }
        }
    }

    public <FT extends Feature> void removeFeatureObserver(@NonNull FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass) {
        this.featureObservers.remove(featureClass, observer);

        for (int i = 0; i < this.features.size(); i++) {
            Feature feature = this.features.get(i);
            if (featureClass.isAssignableFrom(feature.getClass())) {
                observer.onFeatureChange(Objects.requireNonNull(featureClass.cast(feature)), Feature.State.UNAVAILABLE);
            }
        }
    }

    @NonNull
    public State getState() {
        return this.state;
    }

    public boolean isPhysicalDevice(@NonNull DeviceConnectionId connectionId) {
        return this.connectionId != null && this.connectionId.equals(connectionId);
    }

    public boolean isDevice(@NonNull DeviceUid uid) {
        return this.descriptor != null && this.descriptor.deviceUid.equals(uid);
    }

    public boolean equals(@NonNull Device other) {
        return this.getClass().equals(other.getClass()) && this.descriptor != null && other.descriptor != null && this.descriptor.deviceUid.equals(other.descriptor.deviceUid);
    }

    public abstract void open();
    public abstract void close();

    @NonNull
    @Override
    public String toString() {
        return this.descriptor.deviceUid + "@" + this.connectionId;
    }

    //////////////////////////////////////////////////////////
    // Internal stuff meant to be called by implementations //
    //////////////////////////////////////////////////////////

    protected final void setConnectionId(@NonNull DeviceConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    public final DeviceConnectionId getConnectionId() {
        return this.connectionId;
    }

    protected final void setDeviceUid(@NonNull DeviceUid uid) {
        if (connectionId == null) {
            throw new IllegalStateException("Device can not be identified without a connectionId. Did you call setConnectionId(DeviceConnectionId) first?");
        }

        DeviceRepository repo = new DeviceRepository(this.app);
        List<DeviceEntity> entities = repo.getSynchronous(uid);

        // Filter retrieved entities by device type in case the uid was not unique
        for (Iterator<DeviceEntity> it = entities.iterator(); it.hasNext(); ) {
            if (!it.next().isDeviceType(this)) {
                it.remove();
            }
        }

        if (entities.isEmpty()) {
            Log.i(this.getClass().getSimpleName(), "Registering new device with id \"" + uid + "\".");

            if (!uid.isUnique()) {
                // TODO show a warning to the user if device could not be uniquely identified (!newUid.isUnique())
                Log.w(this.getClass().getSimpleName(), "Device uid \"" + uid + "\" is not guaranteed to be unique!");
            }

            this.descriptor = new DeviceEntity(uid, this.app.getString(DeviceType.get(this.getClass()).getTypeName()), this.getClass());
            repo.insertSynchronous(this.descriptor);

            // TODO figure out if we need to read entity again to retrieve it's database id
            this.descriptor = repo.getSynchronous(uid).get(0);

            new NewDeviceNotifier(this.app).notify(this.descriptor);
        } else if (entities.size() == 1) {
            this.descriptor = entities.get(0);
        } else {
            // TODO handle case properly where more than one device entity are returned
            Log.e(this.getClass().getSimpleName(), "More than one device registered with id \"" + uid.toString() + "\".");
            this.close();
        }
    }

    @NonNull
    public DeviceUid getDeviceUid() {
        if (this.descriptor == null) {
            throw new IllegalStateException("Device does not have a DeviceUid yet. Did you call setDeviceUid(DeviceUid) first?");
        }
        return this.descriptor.deviceUid;
    }

    protected final void setState(@NonNull State newState) {
        if (this.state.equals(newState)) {
            return;
        }

        if (this.state.ordinal() > newState.ordinal()) {
            throw new IllegalStateException("Device can not switch from state \"" + this.state + "\" to \"" + newState + "\".");
        }

        if (newState != State.INVALID) {
            if (newState.ordinal() >= State.OPEN.ordinal() && this.connectionId == null) {
                throw new IllegalStateException("Device can not change to state \"" + newState + "\" without a valid connectionId. Did you call setConnectionId(DeviceConnectionId) first?");
            }
            if (newState.ordinal() >= State.READY.ordinal() && this.descriptor == null) {
                throw new IllegalStateException("Device can not change to state \"" + newState + "\" without a valid descriptor. Did you call setDeviceUid(DeviceUid) first?");
            }
        } else {
            while (!this.features.isEmpty()) {
                Feature feature = this.features.remove(0);
                this.removeFeature(feature);
            }
        }

        State previous = this.state;
        this.state = newState;
        for (int i = 0; i < this.stateObservers.size(); i++) {
            this.stateObservers.get(i).onStateChange(this, newState, previous);
        }

        // Clean up after ourselves
        if (this.state == State.INVALID) {
            this.stateObservers.clear();
            this.featureObservers.clear();
        }
    }

    protected final void addFeature(@NonNull Feature feature) {
        if (connectionId == null || this.descriptor == null) {
            throw new IllegalStateException("Device can not notify about features without a connectionId and descriptor. Did you call setConnectionId(DeviceConnectionId) and setDeviceUid(DeviceUid) first?");
        }

        this.features.add(feature);
        feature.setDevice(this);

        this.descriptor.addFeature(feature);
        DeviceRepository repo = new DeviceRepository(this.app);
        repo.update(this.descriptor);

        this.notifyFeatureStateChanged(feature, Feature.State.AVAILABLE);
    }

    protected final void removeFeature(@NonNull Feature feature) {
        if (connectionId == null || this.descriptor == null) {
            throw new IllegalStateException("Device can not notify about features without a connectionId and descriptor. Did you call setConnectionId(DeviceConnectionId) and setDeviceUid(DeviceUid) first?");
        }

        this.features.remove(feature);

        // We never remove features from the DB, so the user can browse them offline

        this.notifyFeatureStateChanged(feature, Feature.State.UNAVAILABLE);
    }

    @SuppressWarnings("unchecked")
    private void notifyFeatureStateChanged(@NonNull Feature feature, @NonNull Feature.State state) {
        ArrayList<FeatureChangeObserver> observers = this.featureObservers.getAssignable(feature.getClass());
        for (FeatureChangeObserver observer : observers) {
            observer.onFeatureChange(feature, state);
        }
    }

    /* TODO: Remove this, if it turns out to be obsolete
    public final ArrayList<Feature> getFeatures() { return this.features; }
    */

    ////////////////////////
    // Coupled interfaces //
    ////////////////////////

    public interface StateObserver {
        void onStateChange(@NonNull Device device, @NonNull State state, @NonNull State previous);
    }

    public interface FeatureChangeObserver<FT> {
        void onFeatureChange(@NonNull FT feature, @NonNull Feature.State state);
    }

}
