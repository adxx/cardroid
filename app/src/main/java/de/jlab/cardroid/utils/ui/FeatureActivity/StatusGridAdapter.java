package de.jlab.cardroid.utils.ui.FeatureActivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedHashMap;

import androidx.annotation.StringRes;

public final class StatusGridAdapter extends BaseAdapter {

    private Context context;
    private LinkedHashMap<Integer, String> dataMap = new LinkedHashMap<>();
    private Integer[] indexList = new Integer[0];

    static class ViewHolder {
        TextView line1;
        TextView line2;
    }

    public StatusGridAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return this.indexList.length;
    }

    @Override
    public Object getItem(int position) {
        return this.dataMap.get(this.indexList[position]);
    }

    @Override
    public long getItemId(int position) {
        return this.indexList[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            final LayoutInflater layoutInflater = LayoutInflater.from(this.context);
            convertView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null);
            holder.line1 = (TextView) convertView.findViewById(android.R.id.text1);
            holder.line2 = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.line1.setText(this.context.getString(this.indexList[position]));
        holder.line2.setText(this.dataMap.get(this.indexList[position]));

        return convertView;
    }

    public void clear() {
        this.dataMap.clear();
        this.indexList = new Integer[0];
    }

    public void update(@StringRes int key, String value) {
        this.dataMap.put(key, value);
        this.indexList = this.dataMap.keySet().toArray(this.indexList.length == this.dataMap.size() ? this.indexList : new Integer[this.dataMap.size()]);
    }

}
