//package kr.co.jjnet.jjsmarthelmet.TabFragment;
//
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import java.util.ArrayList;
//
//import kr.co.jjnet.jjsmarthelmet.R;
//import kr.co.jjnet.jjsmarthelmet.model.jjHistory;
//
///**
// * Created by maro on 2017-07-11.
// */
//
//public class HistoryViewAdapter extends BaseAdapter{
//
//    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
//    private ArrayList<jjHistory> HistoryItemList = new ArrayList<jjHistory>() ;
//
//    // ListViewAdapter의 생성자
//    public HistoryViewAdapter(ArrayList<jjHistory> historyList) {
//        HistoryItemList = historyList;
//    }
//
//    // Adapter에 사용되는 데이터의 개수를 리턴. : 필수 구현
//    @Override
//    public int getCount() {
//        return HistoryItemList.size() ;
//    }
//
//    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        final int pos = position;
//        final Context context = parent.getContext();
//
//        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
//        if (convertView == null) {
//            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            convertView = inflater.inflate(R.layout.listview_item_history, parent, false);
//        }
//
//        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
//        ImageView iconImageView = (ImageView) convertView.findViewById(R.id.HistoryThumbImageView) ;
//        TextView titleTextView = (TextView) convertView.findViewById(R.id.HistoryTitleTextView) ;
//        TextView dateTextView = (TextView) convertView.findViewById(R.id.historyDateTextView) ;
//
//        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
//        jjHistory listViewItem = HistoryItemList.get(position);
//
//        // 아이템 내 각 위젯에 데이터 반영
//        //iconImageView.setImageDrawable(listViewItem.getIcon());
//        titleTextView.setText(listViewItem.getAlertType());
//        dateTextView.setText(listViewItem.getDate());
//
//        return convertView;
//    }
//
//    // 지정한 위치(position)에 있는 데이터와 관계된 아이템(row)의 ID를 리턴. : 필수 구현
//    @Override
//    public long getItemId(int position) {
//        return position ;
//    }
//
//    // 지정한 위치(position)에 있는 데이터 리턴 : 필수 구현
//    @Override
//    public Object getItem(int position) {
//        return HistoryItemList.get(position) ;
//    }
//
////    // 아이템 데이터 추가
////    public void addItem(Drawable icon, String title, String date) {
////        HistoryListViewItem item = new HistoryListViewItem();
////
////        item.setIcon(icon);
////        item.setTitle(title);
////        item.setDate(date);
////
////        HistoryItemList.add(item);
////    }
//
//}
