package de.jlab.cardroid.errors;

import android.content.Context;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.Feature;

public final class ErrorController {

    private SparseArray<Error> errors = new SparseArray<>();

    private DeviceController deviceController;
    private ErrorNotifier errorNotifier;

    private Device.FeatureChangeObserver<ErrorObservable> errorFilter = this::onErrorFeatureChange;
    private ErrorObservable.ErrorListener errorListener = this::onError;


    public ErrorController(@NonNull DeviceController deviceController, @NonNull Context context) {
        this.deviceController = deviceController;
        this.errorNotifier = new ErrorNotifier(context);
        deviceController.subscribeFeature(this.errorFilter, ErrorObservable.class);
    }

    public void dispose() {
        this.deviceController.unsubscribeFeature(this.errorFilter, ErrorObservable.class);
    }

    private void onError(int errorNumber) {
        Error error = this.errors.get(errorNumber);
        if (error == null) {
            error = new Error(errorNumber);
            this.errors.put(errorNumber, error);
        }
        error.count(errorNumber);
        this.errorNotifier.onError(error);
    }

    private void onErrorFeatureChange(@NonNull ErrorObservable feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            feature.addListener(this.errorListener);
        } else {
            feature.removeListener(this.errorListener);
        }
    }

}
