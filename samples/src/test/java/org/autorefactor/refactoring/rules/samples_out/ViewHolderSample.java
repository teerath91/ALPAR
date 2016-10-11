package org.autorefactor.refactoring.rules.samples_out;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public abstract class ViewHolderSample extends BaseAdapter {
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class Adapter1 extends ViewHolderSample {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
    
    public static class Adapter2 extends ViewHolderSample {
        LayoutInflater mInflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	if (convertView == null) {
        		convertView = mInflater.inflate(R.layout.your_layout, null);
        	}
            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText("Position " + position);

            return convertView;
        }
    }
    
    private static class R {
        public static class layout {
            public static final int your_layout = 2;
        }
        public static class id {
            public static final int text = 2;
        }
    }
}