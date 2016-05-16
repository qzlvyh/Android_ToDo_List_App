package com.imaginat.androidtodolist.customlayouts;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.imaginat.androidtodolist.R;
import com.imaginat.androidtodolist.businessModels.IListItem;
import com.imaginat.androidtodolist.businessModels.ListManager;
import com.imaginat.androidtodolist.businessModels.ListTitle;

import java.util.ArrayList;

/**
 * Created by nat on 4/26/16.
 */
public class MainListFragment extends Fragment implements ReminderListRecycleAdapter.IHandleListClicks {

    private TextView anchorTextView;
    private static String TAG= MainListFragment.class.getName();
    ReminderListRecycleAdapter mAdapter;
    ArrayList<ListTitle> mReminders;
    private int lastFirstIndex;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View  view= inflater.inflate(R.layout.main_list_fragment, container, false);
        RelativeLayout rl = (RelativeLayout)view.findViewById(R.id.topPlaceHolder);
        anchorTextView = (TextView)rl.findViewById(R.id.listItemTextView);
        //mAddListOverlayView = (TextView) mTheAddingLayout.findViewById(R.id.addListEditText);
        mAdapter = new ReminderListRecycleAdapter((Context)getActivity(),mReminders=new ArrayList<ListTitle>(),this);
        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.theRecyclerView);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        for(int i=0;i<20;i++){
            ListTitle r = new ListTitle();
            r.setText("LIST "+i);
            mReminders.add(r);
        }



        mAdapter.notifyDataSetChanged();

        //set height and width

        //mAddListOverlayView.setHeight(itemFromList.getHeight());

        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                Log.d(TAG,"VALUE OF dy"+dy);
                if(dy>0) {
                    //test, just erase anything at first position
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    View viewToHide = layoutManager.getChildAt(0);
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    if (viewToHide != null) {
                        IListItem listItem = MainListFragment.this.mAdapter.getItem(firstVisible);
                        anchorTextView.setText(listItem.getText());
                        anchorTextView.setTextColor(Color.rgb(0, 0, 0));

                        TextView tv = (TextView) viewToHide.findViewById(R.id.listItemTextView);
                        String checkingText = tv.getText().toString();
                        Log.d(TAG, "checking the reference to viewToHide, the text it has is " + checkingText);
                        viewToHide.setBackgroundColor(Color.argb(0, 255, 255, 255));
                        ((TextView) viewToHide.findViewById(R.id.listItemTextView)).setTextColor(Color.argb(0, 0, 0, 0));
                        //viewToHide.findViewById(R.id.competedRadioButton).setVisibility(View.INVISIBLE);
                    }
                }else if(dy<0){
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();


                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    View viewToHide = layoutManager.getChildAt(0);
                    View viewToShow = layoutManager.getChildAt(1);
                    if (viewToShow != null) {
                        IListItem listItem = MainListFragment.this.mAdapter.getItem(firstVisible>=0?firstVisible:0);
                        Log.d(TAG,"the anchor will show "+listItem.getText());
                        anchorTextView.setText(listItem.getText());
                        anchorTextView.setTextColor(Color.rgb(0, 0, 0));

                        TextView tv = (TextView) viewToShow.findViewById(R.id.listItemTextView);
                        String checkingText = tv.getText().toString();
                        Log.d(TAG, "reference to viewToShow, the text it has is " + checkingText);
                        //viewToShow.setBackgroundColor(Color.argb(255, 255, 255, 255));
                        viewToShow.setBackground(ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.textlines, null));
                        ((TextView) viewToShow.findViewById(R.id.listItemTextView)).setTextColor(Color.argb(255, 0, 0, 0));
                        //viewToShow.findViewById(R.id.competedRadioButton).setVisibility(View.VISIBLE);


                        viewToHide.setBackgroundColor(Color.argb(0, 255, 255, 255));
                        ((TextView) viewToHide.findViewById(R.id.listItemTextView)).setTextColor(Color.argb(0, 0, 0, 0));
                       // viewToHide.findViewById(R.id.competedRadioButton).setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                super.onScrollStateChanged(recyclerView, newState);




                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();


                Log.d(TAG, "inside onScrollStateChanged, first" + layoutManager.findFirstVisibleItemPosition());
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View itemView = recyclerView.getChildAt(0);
                    int top = Math.abs(itemView.getTop());
                    int bottom = Math.abs(itemView.getBottom());
                    int scrollBy = top >= bottom ? bottom : -top;
                    if (scrollBy == 0) {
                        return;
                    }
                    smoothScrollDeferred(scrollBy, (RecyclerView) recyclerView);
                }
                /*else {

                    int currentIndex = layoutManager.findFirstVisibleItemPosition();

                    if (currentIndex > lastFirstIndex) {
                        Log.d(TAG, "SCROLLING UP (firstVisible:" + currentIndex + " vs lastFirstINdex:" + lastFirstIndex);
                        scrollingUp(currentIndex,layoutManager);
                    } else if (currentIndex < lastFirstIndex) {
                        Log.d(TAG, "SCROLLING DOWN (firstVisible:" + currentIndex + " vs lastFirstINdex:" + lastFirstIndex);
                        scrollingDown(currentIndex,layoutManager);
                    } else {
                        Log.d(TAG, "NO SCROLLING");
                    }
                    lastFirstIndex = currentIndex;


                }

*/
            }
        });
        return view;
    }

    private void smoothScrollDeferred(final int scrollByF,
                                      final RecyclerView viewF) {
        final Handler h = new Handler();
        h.post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                viewF.smoothScrollBy(0, scrollByF);

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        ListManager listManager = ListManager.getInstance(getContext());
        listManager.updateAllListTitles();
        ArrayList<ListTitle>titles =listManager.getListTitles();
        mAdapter.setToRemindersArray(titles);
        mAdapter.notifyDataSetChanged();

    }

    @Override
    public void handleClick(String data) {

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ActionListFragment alf = new ActionListFragment();
        Log.d(TAG,"List id is "+data);
        alf.setListId(data);
            ft.replace(R.id.my_frame, alf);
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            ft.addToBackStack(null);
        ft.commit();
    }
}
