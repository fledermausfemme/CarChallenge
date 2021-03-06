package com.red.carchallenge.view.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.red.carchallenge.App;
import com.red.carchallenge.R;
import com.red.carchallenge.injection.locationdetailactivity.DaggerLocationDetailActivityComponent;
import com.red.carchallenge.injection.locationdetailactivity.LocationDetailActivityComponent;
import com.red.carchallenge.injection.locationdetailactivity.LocationDetailActivityModule;
import com.red.carchallenge.model.LocationResult;
import com.red.carchallenge.util.Utils;
import com.red.carchallenge.viewmodel.LocationDetailViewModel;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class LocationDetailActivity extends BaseActivity {

    public static final String ARG = "LOCATION";
    public static final String MAP = "MAP";

    @BindView(R.id.image_map)
    MapView mapImage;
    @BindView(R.id.text_arrival_time)
    TextView arrivalTimeText;
    @BindView(R.id.text_title)
    TextView titleText;
    @BindView(R.id.text_address)
    TextView addressText;
    @BindView(R.id.text_latitude)
    TextView latitudeText;
    @BindView(R.id.text_longitude)
    TextView longitudeText;

    private LocationResult locationResult;
    private LocationDetailViewModel viewModel;
    private CompositeSubscription subscriptions = Subscriptions.from();
    private BehaviorSubject<GoogleMap> mapSubject = BehaviorSubject.create();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);
        ButterKnife.bind(this);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP);
        }
        mapImage.onCreate(mapViewBundle);

        LocationDetailActivityComponent component = DaggerLocationDetailActivityComponent
                .builder()
                .locationDetailActivityModule(new LocationDetailActivityModule(this))
                .applicationComponent(App.get(this).getApplicationComponent())
                .build();

        component.injectLocationDetailActivity(this);

        locationResult = getIntent().getParcelableExtra(ARG);
        viewModel = new LocationDetailViewModel(locationResult);
        setTitle(locationResult.getName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        init();
    }

    /**
     * According to MapView documentation:
     * "Users of this class must forward all the life cycle methods from the Activity or Fragment
     * containing this view to the corresponding ones in this class."
     */
    @Override
    public void onResume() {
        super.onResume();
        mapImage.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapImage.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapImage.onDestroy();
        subscriptions.unsubscribe();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP, mapViewBundle);
        }
        mapImage.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapImage.onLowMemory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void init() {
        initViews();
        initSubscriptions();
    }

    private void initViews() {
        arrivalTimeText.setText(getTimeUntilArrivalString());
        titleText.setText(viewModel.getName());
        addressText.setText(viewModel.getAddress());
        latitudeText.setText(String.format("%.2f", viewModel.getLatitude()));
        longitudeText.setText(String.format("%.2f", viewModel.getLongitude()));
    }

    private void initSubscriptions() {
        subscriptions.addAll(
                subscribeToMapReadyObservable(),
                subscribeToMapMarker());
    }

    private Subscription subscribeToMapReadyObservable() {
        return Observable.unsafeCreate((Observable.OnSubscribe<GoogleMap>) this::getMap)
                .subscribe(mapSubject);
    }

    private Subscription subscribeToMapMarker() {
        return mapSubject.subscribe(this::addMarkerToMap);
    }

    private void getMap(final Subscriber<? super GoogleMap> subscriber) {
        mapImage.getMapAsync(subscriber::onNext);
    }

    private void addMarkerToMap(GoogleMap map) {
        LatLng position = new LatLng(locationResult.getLatitude(), locationResult.getLongitude());
        map.addMarker(new MarkerOptions().position(position));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 18));
    }

    public String getTimeUntilArrival(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        int option = Utils.getTimeUntilArrivalCategory(millis);

        switch (option) {
            case Utils.TIME_MINUTES:
                return String.format("%d %s", minutes, getMinutesString(minutes));
            case Utils.TIME_HOURS:
                long remainderMinutes = minutes - (hours * 60);
                return String.format("%d %s %d %s", hours, getHoursString(hours), remainderMinutes, getMinutesString(remainderMinutes));
            case Utils.TIME_ERROR:
            default:
                return getResources().getString(R.string.arrival_error);
        }
    }

    public String getTimeUntilArrivalString() {
        return getTimeUntilArrival(viewModel.getTimeUntilArrivalInMillis());
    }

    private String getHoursString(long quantity) {
        return getResources().getQuantityString(R.plurals.hours, (int) quantity);
    }

    private String getMinutesString(long quantity) {
        return getResources().getQuantityString(R.plurals.minutes, (int) quantity);
    }

}
