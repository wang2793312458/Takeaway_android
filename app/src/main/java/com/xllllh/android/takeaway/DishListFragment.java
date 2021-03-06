package com.xllllh.android.takeaway;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DishListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DishListFragment extends Fragment {
    private static final String ARG_SHOP_JSON = "shop_json";

    private StickyListHeadersListView stickyList;

    private JSONObject shop_json;
    private String shopId;
    private String price2Send;
    private Float price2Discount;
    private Float discount;
    private ImageView cart_image;
    private TextView cart_price;
    private Button cart_button;
    private float priceSum;
    private HashMap<String, Integer> cart_dishes = new HashMap<>();
    private HashMap<String, String> dish_json = new HashMap<>();
    private ArrayList<Integer> dishCount;
    public DishListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param shop_json JSON Object of shop.
     * @return A new instance of fragment DishListFragment.
     */
    public static DishListFragment newInstance(JSONObject shop_json) {
        DishListFragment fragment = new DishListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SHOP_JSON, shop_json.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            try {
                shop_json = new JSONObject(getArguments().getString(ARG_SHOP_JSON));
                shopId = Utils.getValueFromJSONObject(shop_json,"id","0");
                price2Send = Utils.getValueFromJSONObject(shop_json, "price_tosend","1");
                String []dis =Utils.getValueFromJSONObject(shop_json,"discount","10-0").split("-");
                price2Discount = Float.parseFloat(dis[0]);
                discount = Float.parseFloat(dis[1]);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        priceSum = 0;
        LoadShopDetailTask detailTask = new LoadShopDetailTask(shopId);
        detailTask.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_dish_list, container, false);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        stickyList = (StickyListHeadersListView) view.findViewById(R.id.dish_list);
        cart_image = (ImageView) view.findViewById(R.id.cart_image);
        cart_price = (TextView) view.findViewById(R.id.cart_price_sum);
        cart_button = (Button) view.findViewById(R.id.cart_order_button);
        cart_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),NewOrderActivity.class);
                intent.putExtra("dish_count",cart_dishes);
                intent.putExtra("dish_json",dish_json);
                if (priceSum >= price2Discount) {
                    intent.putExtra("discount", discount.toString());
                }
                else
                    intent.putExtra("discount","不满足优惠要求");
                startActivity(intent);
            }
        });
        setCartButton();
        return view;
    }

    protected void cartAddItem(String id,JSONObject dish,float price) {
        if (!dish_json.containsKey(id))
            dish_json.put(id,dish.toString());
        if (cart_dishes.containsKey(id)) {
            int cnt = cart_dishes.get(id);
            cart_dishes.remove(id);
            cart_dishes.put(id,cnt+1);
        }
        else {
            cart_dishes.put(id,1);
        }
        priceSum += price;
        cart_price.setText(String.format("￥%.2f",priceSum));
        setCartImage();
        setCartButton();
    }

    protected void cartRemoveItem(String id,float price) {
        int cnt = cart_dishes.get(id);
        cart_dishes.remove(id);
        cart_dishes.put(id,cnt-1);
        priceSum -= price;
        if (priceSum > 0)
            cart_price.setText(String.format("￥%.2f",priceSum));
        else
            cart_price.setText(R.string.cart_no_dish);
        setCartImage();
        setCartButton();
    }

    protected void setCartImage() {
        if (priceSum>0)
            cart_image.setImageResource(R.mipmap.ic_cart_active);
        else
            cart_image.setImageResource(R.mipmap.ic_cart_inactive);
    }

    protected void setCartButton() {
        if (priceSum>=Integer.parseInt(price2Send)) {
            cart_button.setBackground(getResources().getDrawable(R.color.colorPrimary,null));
            cart_button.setText("去结算");
            cart_button.setTextColor(ContextCompat.getColor(getActivity(),R.color.colorTextInPrimaryColor));
            cart_button.setEnabled(true);
        } else {
            cart_button.setBackground(getResources().getDrawable(R.color.colorDarkGrey,null));
            cart_button.setText(String.format("￥%s起送",price2Send));
            cart_button.setTextColor(ContextCompat.getColor(getActivity(), android.R.color.white));
            cart_button.setEnabled(false);
        }
    }

    class LoadShopDetailTask extends AsyncTask<Void,Void,Boolean> {

        String shopId;
        JSONObject shopDetail;
        List<JSONObject> dishList;
        HashMap<String,String> dishType;

        LoadShopDetailTask(String shopId) {
            this.shopId = shopId;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                shopDetail = ShopUtils.Shop.getShopDetail(shopId);
                dishType = ShopUtils.Shop.getDishTypeList(shopDetail.getJSONArray("dishtype"));
                dishList = ShopUtils.Shop.getDishList(shopId);
                return true;
            }catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {

                dishCount = new ArrayList<>();
                for (int i=0;i<dishList.size();i++) {
                    dishCount.add(0);
                }
                final StickListAdapter adapter = new StickListAdapter(getActivity(), dishList, dishType,dishCount);
                adapter.setButtonOnClickListener(new StickListAdapter.ButtonOnClickListener() {

                    @Override
                    public void plus(int position, StickListAdapter.ViewHolder holder, JSONObject dish) {

                        Integer numInt = dishCount.get(position);
                        numInt++;
                        cartAddItem(Utils.getValueFromJSONObject(dish,"id","0"), dish,
                                Float.parseFloat(Utils.getValueFromJSONObject(dish,"price","0")));
                        dishCount.set(position,numInt);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void minus(int position, StickListAdapter.ViewHolder holder, JSONObject dish) {
                        Integer numInt = dishCount.get(position);
                        if (numInt>0)
                        {
                            numInt--;
                            cartRemoveItem(Utils.getValueFromJSONObject(dish,"id","0"),
                                    Float.parseFloat(Utils.getValueFromJSONObject(dish,"price","0")));
                            dishCount.set(position,numInt);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
                stickyList.setAdapter(adapter);
            }
        }
    }
}
