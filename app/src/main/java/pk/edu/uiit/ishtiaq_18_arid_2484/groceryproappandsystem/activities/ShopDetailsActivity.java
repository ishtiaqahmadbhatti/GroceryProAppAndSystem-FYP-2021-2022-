package pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.Constants;
import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.DataBaseHelper;
import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.R;
import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.adapters.AdapterCartItem;
import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.adapters.AdapterProductUser;
import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.models.ModelCartItem;
import pk.edu.uiit.ishtiaq_18_arid_2484.groceryproappandsystem.models.ModelProduct;

public class ShopDetailsActivity extends AppCompatActivity {
    // Declaring  Shop Details Activity  UI Views
    ImageView shopIv;
    TextView shopNameTv, phoneTv, emailTv, openCloseTv, deliveryFeeTv, addressTv, filteredProductsTv, cartCountTv;
    ImageButton callBtn, mapBtn, cartBtn, backBtn, filterProductBtn;
    EditText searchProductEt;
    RecyclerView productsRv;

    String shopUid;
    String myLatitude, myLongitude, myPhone;
    String shopName, shopEmail, shopPhone, shopAddress, shopLatitude, shopLongitude;
    public String deliveryFee;

    // FirebaseAuth
    private FirebaseAuth firebaseAuth;

    // Progress Dialog
    private ProgressDialog progressDialog;

    ArrayList<ModelProduct> productsList;
    AdapterProductUser adapterProductUser;

    // Cart
    ArrayList<ModelCartItem> cartItemList;
    AdapterCartItem adapterCartItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

        ViewsInitialization();
        ViewsPerformanceActions();
        cartCount();
    }

    // UI Views Initialization
    public void ViewsInitialization() {
        // Initialization Of Views
        shopIv = findViewById(R.id.shopIv);
        shopNameTv = findViewById(R.id.shopNameTv);
        phoneTv = findViewById(R.id.phoneTv);
        emailTv = findViewById(R.id.emailTv);
        openCloseTv = findViewById(R.id.openCloseTv);
        deliveryFeeTv = findViewById(R.id.deliveryFeeTv);
        addressTv = findViewById(R.id.addressTv);
        filteredProductsTv = findViewById(R.id.filteredProductsTv);
        callBtn = findViewById(R.id.callBtn);
        mapBtn = findViewById(R.id.mapBtn);
        cartBtn = findViewById(R.id.cartBtn);
        backBtn = findViewById(R.id.backBtn);
        filterProductBtn = findViewById(R.id.filterProductBtn);
        searchProductEt = findViewById(R.id.searchProductEt);
        productsRv = findViewById(R.id.productsRv);
        cartCountTv = findViewById(R.id.cartCountTv);

        // Get Uid Of The Shop From Intent
        shopUid = getIntent().getStringExtra("shopUid");

        // Initialization Of FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        // Initialization Of Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadMyInfo();
        loadShopDetails();
        loadShopProducts();
        // Each Shop Have Its Own Products and Orders, So If User Add Items To Cart And Go Back
        // And Open Cart Different Shop Then Cart Should Bbe Different
        // So Delete Cart Data Whenever User Open This Activity
        deleteCartData();

    }

    private void deleteCartData() {
        DataBaseHelper dataBaseHelper = new DataBaseHelper(this);
    }

    public void cartCount(){
        // Keep It Public So We Can Access In Adapter
        // Get Cart Count
        DataBaseHelper dataBaseHelper = new DataBaseHelper(this);
        int count = dataBaseHelper.cartCount().getCount();
        if (count<=0){
            // No Item In Cart, Hide Cart Count TextView
            cartCountTv.setVisibility(View.GONE);
        }
        else {
            // Have Items In Cart, Hide Cart Count TextView and Set Count
            cartCountTv.setVisibility(View.VISIBLE);
            cartCountTv.setText("" + count); // Concatenate With String, Because We can't Set Integer In TextView
        }

    }

    // UI Views Performance Actions
    public void ViewsPerformanceActions() {

        // Search
        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterProductUser.getFilter().filter(s);
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go Previous Activity
                onBackPressed();
            }
        });

        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show Cart Dialog
                showCartDialog();
            }
        });

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialPhone();
            }
        });


        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        filterProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ShopDetailsActivity.this);
                builder.setTitle("Choose Category")
                        .setItems(Constants.productCategories1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Get Selected Item
                                String selected = Constants.productCategories1[which];
                                filteredProductsTv.setText(selected);
                                if (selected.equals("All")){
                                    // Load All
                                    loadShopProducts();
                                }
                                else {
                                    //Load Filtered
                                    adapterProductUser.getFilter().filter(selected);
                                }
                            }
                        })
                        .show();
            }
        });

    }

   public double allTotalPrice = 0.00;

    // Need To Access Theses Views In Adapter So Making Public
    public TextView sTotalTv, dFeeTv, allTotalPriceTv;

    private void showCartDialog() {
        // Initialization List
        cartItemList = new ArrayList<>();

        // Inflate Cart Layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);

        // Initialization Views
        TextView shopNameTv = view.findViewById(R.id.shopNameTv);
        RecyclerView cartItemsRv = view.findViewById(R.id.cartItemsRv);
        sTotalTv = view.findViewById(R.id.sTotalTv);
        dFeeTv = view.findViewById(R.id.dFeeTv);
        allTotalPriceTv = view.findViewById(R.id.totalTv);
        Button checkoutBtn = view.findViewById(R.id.checkoutBtn);

        // Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set View To Dialog
        builder.setView(view);

        shopNameTv.setText(shopName);

        DataBaseHelper dataBaseHelper = new DataBaseHelper(this);
        // Get All Data From Database (Sqlite)
        Cursor res = dataBaseHelper.getCartData();
        if (res.getCount() == 0){
            Toast.makeText(this, "Cart Is Empty", Toast.LENGTH_SHORT).show();
            return;
        }
        while(res.moveToNext()){

            String id = res.getString(0);
            String pId = res.getString(1);
            String name = res.getString(2);
            String price = res.getString(3);
            String cost = res.getString(4);
            String quantity = res.getString(5);

           //allTotalPriceTv = allTotalPrice + Double.parseDouble(cost);
            ModelCartItem modelCartItem = new ModelCartItem(
                    "" + id,
                    ""+pId,
                    ""+name,
                    ""+price,
                    ""+cost,
                    ""+quantity
            );
            cartItemList.add(modelCartItem);
        }
        // Setup Adapter
        adapterCartItem = new AdapterCartItem(this,cartItemList);
        // Set To Recyclerview
        cartItemsRv.setAdapter(adapterCartItem);

        dFeeTv.setText("$" + deliveryFee);
        sTotalTv.setText("$" + String.format("%.2f", allTotalPrice));
        allTotalPriceTv.setText("$"+(allTotalPrice + Double.parseDouble((deliveryFee.replace("$","")))));

        // Show Dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        // Reset Total Price On Dialog Dismiss
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                allTotalPrice = 0.00;
            }
        });

        // Place Order
        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // First Validate Delivery Address
                if(myLatitude.equals("") || myLatitude.equals("null") || myLongitude.equals("") || myLongitude.equals("null")){
                    // User Didn't Enter Address In Profile
                    Toast.makeText(ShopDetailsActivity.this, "Please Enter Your Address In Your Profile Before Placing Order", Toast.LENGTH_SHORT).show();
                    return; // Don't Proceed Further
                }
                if(myPhone.equals("") || myPhone.equals("null")){
                    // User Didn't Enter Phone Number In Profile
                    Toast.makeText(ShopDetailsActivity.this, "Please Enter Your Phone Number In Your Profile Before Placing Order", Toast.LENGTH_SHORT).show();
                    return; // Don't Proceed Further
                }
                if (cartItemList.size() == 0){
                    // Cart List IS Empty
                    Toast.makeText(ShopDetailsActivity.this, "No Item In Cart", Toast.LENGTH_SHORT).show();
                    return; // Don't Proceed Further
                }
                submitOrder();
            }
        });

//        StringBuffer buffer = new StringBuffer();
//        while(res.moveToNext()){
//
//            buffer.append("Item_Id :"+ res.getString(0)+"\n\n");
//            buffer.append("Item_PID :"+ res.getString(1)+"\n\n");
//            buffer.append("Item_Name :"+ res.getString(2)+"\n\n");
//            buffer.append("Item_Price_Each :"+ res.getString(3)+"\n\n");
//            buffer.append("Item_Price :"+ res.getString(4)+"\n\n");
//            buffer.append("Item_Quantity :"+ res.getString(5)+"\n\n");
//        }
//
//        AlertDialog.Builder build= new AlertDialog.Builder(ShopDetailsActivity.this);
//        build.setCancelable(true);
//        build.setTitle("Cart Items");
//        build.setMessage(buffer.toString());
//        build.show();
    }

    private void submitOrder() {
        // Show Progress Dialog
        progressDialog.setMessage("Placing Order...");
        progressDialog.show();

        //For Order ID And Order Time
        String timestamp = ""+System.currentTimeMillis();

        String cost = allTotalPriceTv.getText().toString().trim().replace("$", ""); // Remove $ If Contains
        // Add Latitude, Longitude Of User To Each Other | Delete Previous Orders From Firebase or Add Manually To Them

        // Setup Order Data
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("orderId", "" + timestamp);
        hashMap.put("orderTime", "" + timestamp);
        hashMap.put("orderStatus", "In Progress"); // In Progress/Completed/Cancelled
        hashMap.put("orderCost", ""+cost);
        hashMap.put("orderBy", ""+firebaseAuth.getUid());
        hashMap.put("orderTo", ""+shopUid);
        hashMap.put("Latitude", ""+myLatitude);
        hashMap.put("Longitude", ""+myLongitude);

        // Add To Database (Firebase)
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        reference.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Order INFO Added Now And Order Items
                        for (int i=0; i<cartItemList.size(); i++){
                            String pId = cartItemList.get(i).getpId();
                            String id = cartItemList.get(i).getId();
                            String cost = cartItemList.get(i).getCost();
                            String name = cartItemList.get(i).getName();
                            String price = cartItemList.get(i).getPrice();
                            String quantity = cartItemList.get(i).getQuantity();

                            HashMap<String, String> hashMap1 = new HashMap<>();
                            hashMap1.put("pId", pId);
                            hashMap1.put("name", name);
                            hashMap1.put("cost", cost);
                            hashMap1.put("price", price);
                            hashMap1.put("quantity", quantity);

                            reference.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                        }

                        progressDialog.dismiss();
                        Toast.makeText(ShopDetailsActivity.this, "Order Placed Successfully", Toast.LENGTH_SHORT).show();

                        // After Placing Order Open Order Details Page
                        // Open Order Details, We Need To Keys there, orderId, orderTo
                        Intent intent = new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
                        intent.putExtra("orderTo", shopUid);
                        intent.putExtra("orderId", timestamp);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed Placing Order
                        progressDialog.dismiss();
                        Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openMap() {

        // saddr means Source Address
        // daddr means Destination Address
        String address = "https://maps.google.com/maps?saddr=" +myLatitude+ "," +myLongitude+ "&daddr=" +shopLatitude+ "," +shopLongitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
        startActivity(intent);

    }

    private void dialPhone() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(shopPhone))));
        Toast.makeText(this, ""+shopPhone, Toast.LENGTH_SHORT).show();
    }

    private void loadMyInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot ds: snapshot.getChildren()){

                            // Set User Data
                            String name = ""+ds.child("name").getValue();
                            String email = ""+ds.child("email").getValue();
                            myPhone = ""+ds.child("phone").getValue();
                            String profileImage = ""+ds.child("profileImage").getValue();
                            String accountType = ""+ds.child("accountType").getValue();
                            String city = ""+ds.child("city").getValue();
                            myLatitude = ""+ds.child("latitude").getValue();
                            myLongitude = ""+ds.child("longitude").getValue();

                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadShopDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get Shop Data
                String name = ""+snapshot.child("name").getValue();
                shopName = ""+snapshot.child("shopName").getValue();
                shopEmail = ""+snapshot.child("email").getValue();
                shopPhone = ""+snapshot.child("phone").getValue();
                shopLatitude = ""+snapshot.child("latitude").getValue();
                shopLongitude = ""+snapshot.child("longitude").getValue();
                shopAddress = ""+snapshot.child("address").getValue();
                deliveryFee = ""+snapshot.child("deliveryFee").getValue();
                String profileImage = ""+snapshot.child("profileImage").getValue();
                String shopOpen = ""+snapshot.child("shopOpen").getValue();
                // Set Shop Data
                shopNameTv.setText(shopName);
                emailTv.setText(shopEmail);
                deliveryFeeTv.setText("Delivery Fee: $" + deliveryFee);
                addressTv.setText(shopAddress);
                phoneTv.setText(shopPhone);

                if (shopOpen.equals("true")){
                    openCloseTv.setText("Open");
                }

                else {
                    openCloseTv.setText("Closed");
                }

                try {
                    Picasso.get().load(profileImage).into(shopIv);
                }
                catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadShopProducts() {
        // Initialization List
        productsList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Clear List Before Adding Items
                        productsList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()){
                            ModelProduct modelProduct = ds.getValue(ModelProduct.class);
                            productsList.add(modelProduct);
                        }

                        // Setup Adapter
                        adapterProductUser = new AdapterProductUser(ShopDetailsActivity.this, productsList);
                        // Set Adapter
                        productsRv.setAdapter(adapterProductUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}