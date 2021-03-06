package bin.project.binmanager;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;


import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.GeoApiContext;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;


//import android.support.design.widget.FloatingActionButton;


public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback {

    private static final int GEOFENCE_RADIUS = 100;
    private double[][] latLon = new double[100][2];
    //{{19.026756, 73.055807}, {19.027111, 73.057295}, {19.025488, 73.054763}, {19.026606, 73.055233}, {19.026264, 73.056633}};

    private long[] fillLevelArray = new long[100];

    private Marker marker;
    private Marker locationMarker;

    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;

    private DatabaseReference myRefBin;
    private DatabaseReference myRefUsers;

    private String TAG = MapsActivity.class.getSimpleName();
    private double lat, lng;
    private long fill_level;
    private String marked_for_pickup="";
    private String disp_name = "";
    private String email = "";

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    boolean dashboard;
    private ProgressDialog pd;

    private boolean addClickFlag = false;
    private boolean deleteClickFlag = false;
    private boolean userlocationFlag = false;
    private boolean setFlag = false;
    private boolean selectFlag = false;

    private List<LatLng> waypoints = new ArrayList<>();

    private Toolbar toolbar;
    private AccountHeader headerResult;

    private int getBinId;

    LocationManager locationManager;
    private ValueEventListener fillListener;

    private Vector vector;
    private SparseArray<String > filledBins = new SparseArray<String>();
    private ArrayList<String > markedBins = new ArrayList<String>();

    private FloatingActionButton floatingActionButton, fabSelect, fabFilled;
    private FloatingActionMenu floatingActionMenu;



    private Polyline polyline;
    private List<Polyline> polylines = new ArrayList<Polyline>();
    private List<Marker> listOfMarkerForFilledBins = new ArrayList<Marker>();
    private List<Marker> listOfMarkerForSelectedBins = new ArrayList<Marker>();

    private View infoView;
    private Marker markerForFilledBins;
    private GeofencingClient geofencingClient;
    private Criteria criteria;
    public static Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        LocalBroadcastManager.getInstance(this).registerReceiver(

                mMessageReceiver, new IntentFilter("bina"));


        toolbar = findViewById(R.id.map_toolbar);
        setSupportActionBar(toolbar);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionMenu = findViewById(R.id.floatingActionMenu);
        fabFilled = findViewById(R.id.filledBinsButton);
        fabSelect = findViewById(R.id.selectBinsButton);

        vector = new Vector();

        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();


        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myRefBin = database.getReference("Bins");
        myRefUsers = database.getReference("Users").child(firebaseAuth.getCurrentUser().getUid());
        Toast.makeText(this, "Logged in as " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();

        myRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Users users = dataSnapshot.getValue(Users.class);
                disp_name = users.display_name;
                email = users.email;
                headerResult.addProfiles(
                        new ProfileDrawerItem().withName(disp_name).withEmail(email)
                );
                createDrawer();


            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        createDrawer();


        //for toolbar transparency
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        geofencingClient = LocationServices.getGeofencingClient(this);


    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
    
            //Toast.makeText(context, "action  " + action, Toast.LENGTH_SHORT).show();

        }

    };



    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.directionsApiKey))
                .queryRateLimit(3)
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build();
        return geoApiContext;
    }


    @Override
    public void onMapReady(final GoogleMap map) {
        mMap = map;
        mMap.getUiSettings().setMapToolbarEnabled(false);


        vector.add(0, 0);

        if (mMap != null) {
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    infoView = getLayoutInflater().inflate(R.layout.bin_info_window, null);
                    TextView bininfotitle = infoView.findViewById(R.id.binIdInfo);
                    TextView bininfoFillLevel = infoView.findViewById(R.id.binFlInfo);
                    TextView binFillLabel = infoView.findViewById(R.id.fillLabel);
                    TextView binMarked = infoView.findViewById(R.id.binMarked);
                    try {
                         // though marker.infowindow shows data in info window
                        if(marker.getTitle().contains("waypoint")){
                            bininfotitle.setText("waypoint");
                            binFillLabel.setVisibility(View.GONE);
                            bininfoFillLevel.setVisibility(View.GONE);
                            return infoView;
                        }
                        if (marker.getTag().toString().contains("markedByOther")) {
                            bininfoFillLevel.setText(marker.getSnippet());
                            binMarked.setVisibility(View.VISIBLE);
                            binMarked.setText("Marked for pickup");

                            bininfotitle.setTextSize(13);
                            return infoView;
                        }
                        if (marker.getTag().toString().contains("markedByOther")) {
                            bininfoFillLevel.setText(marker.getSnippet());
                            binMarked.setVisibility(View.VISIBLE);
                            binMarked.setText("Marked for pickup");

                            bininfotitle.setTextSize(13);
                            return infoView;
                        }


                        if (marker.getTitle().contains("User")) {
                            bininfotitle.setText("User");
                            binFillLabel.setVisibility(View.GONE);
                            bininfoFillLevel.setVisibility(View.GONE);
                            return infoView;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "getInfoContents: " + marker.getTitle());
                    bininfotitle.setText("Bin " + marker.getTitle());
                    bininfoFillLevel.setText(marker.getSnippet());

                    return infoView;
                }
            });

        }

        myRefBin.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int i = 1, id;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Bins bins = snapshot.getValue(Bins.class);
                    //Log.d(TAG, "onDataChange: bins " + bins.lat + " mapr" + bins.marked_for_pickup );
                    lat = bins.lat;
                    lng = bins.lng;
                    //marked_for_pickup = bins.marked_for_pickup;

                    id = Integer.parseInt(snapshot.getKey());
                    fill_level = bins.fill_level;

                    /*if (fill_level >= 80) {
                        waypoints.add(new LatLng(lat, lng));
                    }*/
                    marker = createMarker(map, lat, lng, fill_level, id);
                    marker.setTag("0");
                    while (i != id) {
                        vector.add(i, 1);
                        i++;
                    }
                    vector.add(id, marker);
                    changeBinMarker((Marker) vector.elementAt(id), map, marker.getTitle());
                    i++;
//                    latLon[i][0] = lat;
//                    latLon[i][1] = lng;
                }

               /* for (int j = 0; j < dataSnapshot.getChildrenCount(); j++) {
                    addDistanceOfBinsToDb(dataSnapshot.getChildrenCount(), latLon[j][0], latLon[j][1], j);
                }

              changeBinMarker(marker, map, marker.getTitle());
                getUserLocation();*/

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);

            }
        } else {
            mMap.setMyLocationEnabled(true);
        }
        //  showDirection(userLocation);


        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(19.026738, 73.055215), 16));

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (addClickFlag) {

                    addClickFlag = false;
                    //floatingActionButton.setImageResource(R.drawable.ic_show_direction);
                    floatingActionButton.setVisibility(View.GONE);
                    floatingActionMenu.setVisibility(View.VISIBLE);
                    return;
                }
                if (deleteClickFlag) {
                    deleteClickFlag = false;
                    //floatingActionButton.setImageResource(R.drawable.ic_show_direction);
                    floatingActionButton.setVisibility(View.GONE);
                    floatingActionMenu.setVisibility(View.VISIBLE);
                    return;
                }
                if (selectFlag) {
                    selectFlag = false;
                    if (!waypoints.isEmpty()) {
                        getUserLocation(map);
                    }

                    floatingActionButton.setImageResource(R.drawable.ic_fab_cancel);
                    userlocationFlag = true;
                    for (Marker marker : listOfMarkerForSelectedBins) {
                        marker.remove();
                        Log.d(TAG, "removing ");
                    }
                    return;
                } else {
                    userlocationFlag = false;
                    floatingActionButton.setImageResource(R.drawable.ic_show_direction);
                    for (Polyline line : polylines) {
                        line.remove();
                    }
                    polylines.clear();
                    for (Marker marker : listOfMarkerForFilledBins) {
                        marker.remove();
                        Log.d(TAG, "removing ");
                    }
                    for(String s: markedBins)
                    {
                        myRefBin.child(s).child("marked_for_pickup").setValue(0);
                    }
                    markedBins.clear();

                    floatingActionButton.setVisibility(View.GONE);
                    floatingActionMenu.setVisibility(View.VISIBLE);
                }
            }
        });

        fabFilled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markedBins.clear();
                for (Object obj : vector) {
                    if (obj instanceof Marker) {
                        if (Integer.parseInt(((Marker) obj).getSnippet()) >= 80 && !((Marker) obj).getTag().equals("markedByOther")) {
                            markedBins.add(((Marker) obj).getTitle());
                            myRefBin.child(((Marker) obj).getTitle()).child("marked_for_pickup").setValue(firebaseAuth.getUid()); //
                            waypoints.add((((Marker) obj).getPosition()));
                        }
                    }
                }
                getUserLocation(map);
                floatingActionButton.setImageResource(R.drawable.ic_fab_cancel);
                userlocationFlag = true;
                floatingActionButton.setVisibility(View.VISIBLE);
                floatingActionButton.setLabelText("Cancel");
                floatingActionMenu.setVisibility(View.GONE);
                floatingActionMenu.close(false);


            }
        });

        fabSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFlag = true;
                floatingActionButton.setImageResource(R.drawable.ic_proceed);
                floatingActionButton.setVisibility(View.VISIBLE);
                floatingActionButton.setLabelText("Proceed");
                floatingActionMenu.setVisibility(View.GONE);
                floatingActionMenu.close(false);

            }
        });


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latlng) {
                if (addClickFlag) {
                    if (vector.indexOf(1) != -1) {
                        getBinId = vector.indexOf(1);
                        setFlag = true;
                    } else {
                        setFlag = false;
                        getBinId = vector.size();
                    }
                    final LatLng point = latlng;
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    String binId = String.valueOf(getBinId);
                                    marker = createMarker(mMap, point.latitude, point.longitude, 0, getBinId);
                                    Log.d(TAG, "onClick: Vector size" + vector.size());
                                    Log.d(TAG, "onClick: Vector" + vector);
                                    if (setFlag)
                                        vector.set(getBinId, marker);
                                    else
                                        vector.add(vector.size(), marker);
                                    changeBinMarker(marker, mMap, marker.getTitle());
                                    myRefBin.child(binId).child("lat").setValue(point.latitude);
                                    myRefBin.child(binId).child("lng").setValue(point.longitude);
                                    myRefBin.child(binId).child("fill_level").setValue(0);
                                    myRefBin.child(binId).child("marked_for_pickup").setValue("0");
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MapsActivity.this, R.style.MaterialBaseTheme_Light_AlertDialog));
                    builder.setMessage("Add bin " + getBinId + "\nat location: " + "\n(" + point.latitude + ", " + point.longitude + ")").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker1) {
                if (deleteClickFlag) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    myRefBin.child(marker1.getTitle()).child("fill_level").removeEventListener(fillListener);
                                    vector.setElementAt(1, vector.indexOf(marker1));
                                    marker1.remove();
                                    myRefBin.child(marker1.getTitle()).removeValue();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(MapsActivity.this, R.style.MaterialBaseTheme_Light_AlertDialog));
                    builder.setMessage("Remove bin " + marker1.getTitle()).setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();

                    return false;

                } else if (selectFlag) {

//                    if(!marker1.getTag().equals("0"))
//                        return false;
                    waypoints.add(marker1.getPosition());
                    Marker wpMarker = map.addMarker(new MarkerOptions().position(marker1.getPosition()).title("waypoint"));
                    markedBins.add(marker1.getTitle());
                    myRefBin.child(marker1.getTitle()).child("marked_for_pickup").setValue(firebaseAuth.getUid()); //
                    // Log.d(TAG, "onMarkerClick: "+ marker1.getTitle());
                    listOfMarkerForSelectedBins.add(wpMarker);
                }

                return false;

            }
        });


    }


    protected Marker createMarker(GoogleMap map, double latitude, double longitude, long fill_level, int id) {
        if (fill_level >= 80)
            return map.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .anchor(0.5f, 0.5f)
                    .title(String.valueOf(id))
                    .snippet(String.valueOf(fill_level))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_full)));
        else
            return map.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .anchor(0.5f, 0.5f)
                    .title(String.valueOf(id))
                    .snippet(String.valueOf(fill_level))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_normal)));
    }

    private void changeBinMarker(final Marker marker, final GoogleMap map,final String title) {
        fillListener = myRefBin.child(title).child("fill_level").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                try {
                    String fill_level_string = dataSnapshot.getValue().toString();
                    if (Integer.parseInt(fill_level_string) >= 80) {

                        addLocationAlert(marker.getPosition().latitude, marker.getPosition().longitude, title);
                        marker.setSnippet(fill_level_string);
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_full));
                    } else {
                        marker.setSnippet(fill_level_string);
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin_normal));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        myRefBin.child(title).child("marked_for_pickup").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String markedString = dataSnapshot.getValue().toString();
                try {
                     if(!markedString.equalsIgnoreCase("0") && !markedString.equalsIgnoreCase(firebaseAuth.getUid())) {
                         Log.d(TAG, "onDataChange: marked" + dataSnapshot.getValue());
                         marker.setTag("markedByOther");
                     }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void createDrawer() {
        headerResult = new AccountHeaderBuilder()
                .withActivity(MapsActivity.this)
                .withHeaderBackground(R.color.primary_dark)
                .addProfiles(
                        new ProfileDrawerItem().withName(disp_name).withEmail(email)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();
        if (disp_name.equalsIgnoreCase("Admin")) {
            //Now create your drawer and pass the AccountHeader.Result
            Drawer result = new DrawerBuilder()
                    .withAccountHeader(headerResult)
                    .withActivity(MapsActivity.this)
                    .withToolbar(toolbar)
                    .addDrawerItems(
                            new ExpandableDrawerItem().withIdentifier(1).withName(R.string.drawer_item_manageBins).withSubItems(
                                    new SecondaryDrawerItem().withIdentifier(2).withName(R.string.drawer_item_addBin),
                                    new SecondaryDrawerItem().withIdentifier(3).withName(R.string.drawer_item_removeBin)),
                            new SecondaryDrawerItem().withIdentifier(4).withName(R.string.dashboard).withSelectable(false),
                            new SecondaryDrawerItem().withIdentifier(5).withName(R.string.drawer_item_signout)
                    )
                    .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                        @Override
                        public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                            // do something with the clicked item :D
                            Log.d(TAG, "onItemClick: " + drawerItem.getIdentifier());
                            switch ((int) drawerItem.getIdentifier()) {
                                case 2:

                                    if (!addClickFlag) {
                                        Toast.makeText(MapsActivity.this, "Tap on map to add a bin", Toast.LENGTH_SHORT).show();
                                        floatingActionButton.setVisibility(View.VISIBLE);
                                        floatingActionMenu.setVisibility(View.GONE);
                                        floatingActionMenu.close(false);

                                        floatingActionButton.setImageResource(R.drawable.ic_fab_cancel);
                                        addClickFlag = true;
                                        deleteClickFlag = false;
                                        userlocationFlag = false;
                                        floatingActionButton.setLabelText("Cancel");

                                    } else {
                                        addClickFlag = false;
                                        floatingActionButton.setVisibility(View.GONE);
                                        floatingActionMenu.setVisibility(View.VISIBLE);
                                    }
                                    break;
                                case 3:
                                    if (!deleteClickFlag) {
                                        Toast.makeText(MapsActivity.this, "Tap on the bin to remove it", Toast.LENGTH_SHORT).show();
                                        floatingActionButton.setVisibility(View.VISIBLE);
                                        floatingActionMenu.setVisibility(View.GONE);
                                        floatingActionMenu.close(false);
                                        floatingActionButton.setLabelText("Cancel");
                                        floatingActionButton.setImageResource(R.drawable.ic_fab_cancel);
                                        deleteClickFlag = true;
                                        userlocationFlag = false;
                                        addClickFlag = false;
                                    } else {
                                        deleteClickFlag = false;
                                        floatingActionButton.setImageResource(R.drawable.ic_show_direction);
                                        floatingActionButton.setVisibility(View.GONE);
                                        floatingActionMenu.setVisibility(View.VISIBLE);
                                    }
                                    break;
                                case 4:
                                    dashboard = true;
                                    startActivity(new Intent(MapsActivity.this, Dashboard.class));
                                    break;
                                case 5:
                                    firebaseAuth.signOut();
                                    startActivity(new Intent(MapsActivity.this, LoginActivity.class));

                            }

                            return false;
                        }
                    }).build();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);

        } else {
            //Now create your drawer and pass the AccountHeader.Result
            Drawer result = new DrawerBuilder()
                    .withAccountHeader(headerResult)
                    .withActivity(MapsActivity.this)
                    .withToolbar(toolbar)
                    .addDrawerItems(
                            new SecondaryDrawerItem().withName(R.string.dashboard),
                            new SecondaryDrawerItem().withName(R.string.drawer_item_signout)
                    ).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                        @Override
                        public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                            // do something with the clicked item :D
                            switch (position) {
                                case 1:
                                    dashboard = true;
                                    startActivity(new Intent(MapsActivity.this, Dashboard.class));
                                    break;
                                case 2:
                                    firebaseAuth.signOut();
                                    Intent toLogin = new Intent(MapsActivity.this, LoginActivity.class);
                                    startActivity(toLogin);
                            }
                            return false;
                        }
                    }).build();
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        }
    }


    @SuppressLint("MissingPermission")
    private LatLng getUserLocation(final GoogleMap map) {
  /*      final LatLng[] latLng = new LatLng[1];
        myRefUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                double latitude = user.lat;
                double longitude = user.lng;
                latLng[0] = new LatLng(latitude, longitude);
                if (locationMarker != null)
                    locationMarker.remove();
                locationMarker = map.addMarker(new MarkerOptions()
                        .position(latLng[0]).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user))
                        .title("User")
                        .snippet("334")
                        .anchor(0.5f, 0.5f)


                );
                locationMarker.showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng[0], 15));
                showDirection(latLng[0]);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/
        Location location = locationManager.getLastKnownLocation(locationManager
                .getBestProvider(criteria, false));
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        showDirection(userLocation);
//        myRefUsers.child("lat").setValue(userLocation.latitude);
//        myRefUsers.child("lng").setValue(userLocation.longitude);
        return userLocation;

/*        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                      if(locationMarker!= null)
                          locationMarker.remove();
                       locationMarker = mMap.addMarker(new MarkerOptions()
                               .position(latLng)
                               .title("You")

                       );
                     mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    myRefUsers.child("lat").setValue(latitude);
                    myRefUsers.child("lng").setValue(longitude);

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
        else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng latLng = new LatLng(latitude, longitude);
                    if(locationMarker!= null)
                        locationMarker.remove();
                    locationMarker = mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("You")

                    );
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    myRefUsers.child("lat").setValue(latitude);
                    myRefUsers.child("lng").setValue(longitude);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }*/
    }

    private void showDirection(LatLng userLocation) {
        GoogleDirection.withServerKey(getString(R.string.directionsApiKey))
                .from(userLocation)
                .and(waypoints)
                .to(waypoints.get(waypoints.size() - 1))
                .optimizeWaypoints(true)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            // Do something
                            Log.d(TAG, "onDirectionSuccess: success");
                            Route route = direction.getRouteList().get(0);
                            int legCount = route.getLegList().size();
                            for (int index = 0; index < legCount; index++) {
                                Leg leg = route.getLegList().get(index);
                                markerForFilledBins = mMap.addMarker(new MarkerOptions().position(leg.getStartLocation().getCoordination()).title("waypoint"));
                                listOfMarkerForFilledBins.add(markerForFilledBins);
                                if (index == legCount - 1) {
                                    markerForFilledBins = mMap.addMarker(new MarkerOptions().position(leg.getEndLocation().getCoordination()));
                                    listOfMarkerForFilledBins.add(markerForFilledBins);
                                }
                                List<Step> stepList = leg.getStepList();
                                ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(MapsActivity.this, stepList, 5, Color.RED, 3, Color.BLUE);
                                for (PolylineOptions polylineOption : polylineOptionList) {
                                    polyline = mMap.addPolyline(polylineOption);
                                    polylines.add(polyline);

                                }
                                waypoints.clear();
                            }
                            setCameraWithCoordinationBounds(route);

                        } else {
                            // Do something
                            Log.d(TAG, "onDirectionSuccess: fail ");
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                        Log.d(TAG, "onDirectionFailure: failed");
                    }
                });
    }

    @Override
    public void onBackPressed() {

    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    @SuppressLint("MissingPermission")
    private void addLocationAlert(double lat, double lng, String binNo) {
        if (isLocationAccessPermitted()) {
            requestLocationAccessPermission();
        } else {
            String key = "" + lat + "-" + lng;
            Geofence geofence = getGeofence(lat, lng, key, binNo);
            geofencingClient.addGeofences(getGeofencingRequest(geofence),
                    getGeofencePendingIntent(binNo))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(MapsActivity.this,
                                        "Location alter has been added",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MapsActivity.this,
                                        "Location alter could not be added",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        }
    }

    private void removeLocationAlert() {
        if (isLocationAccessPermitted()) {
            requestLocationAccessPermission();
        } else {
            geofencingClient.removeGeofences(getGeofencePendingIntent(""))
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(MapsActivity.this,
                                        "Location alters have been removed",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MapsActivity.this,
                                        "Location alters could not be removed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    //TODO : send data to service class for displaying in notification
    private PendingIntent getGeofencePendingIntent(String binNo) {
        Intent intent = new Intent(MapsActivity.this, LocationAlertIntentService.class);
        intent.putExtra("binNo", binNo);
        Log.d(TAG, "addLocationAlert: binNo" + binNo);

        return PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }

    private Geofence getGeofence(double lat, double lang, String key, String binNo) {
        return new Geofence.Builder()
                .setRequestId(key)
                .setCircularRegion(lat, lang, GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(10000)
                .build();
    }

    private boolean isLocationAccessPermitted() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestLocationAccessPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    private void pickUp(){

    }




}

 /* private void addDistanceOfBinsToDb(long noOFbins, double lat, double lng, int currentBin) {
        DateTime now = new DateTime();
        try {

            for (int i = 1; i <= noOFbins; i++) {
                if (lat != latLon[i - 1][0]) {
                    DirectionsResult result = DirectionsApi.newRequest(getGeoContext())
                            .mode(TravelMode.DRIVING)
                            .origin(new com.google.maps.model.LatLng(lat, lng))
                            .destination(new com.google.maps.model.LatLng(latLon[i - 1][0], latLon[i - 1][1]))
                            .departureTime(now)
                            .await();
                    int binNumber = i;
                    myRefBin.child(String.valueOf(currentBin + 1))
                            .child("distance from " + binNumber)
                            .setValue(result.routes[0].legs[0].distance.humanReadable);
                }
            }

                    Log.d(TAG, "Time :" + result.routes[0].legs[0].duration.humanReadable + " Distance :"
                      + result.routes[0].legs[0].distance.humanReadable);

        } catch (ApiException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();

        }
    }
    */
