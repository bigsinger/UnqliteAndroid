package com.xsl.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xsl.unqlite.UnqliteArray;

import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    public static final int TestCount = 1000000;
    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    private RecyclerView recycler;
    private UnqliteArray unqliteArray;
    private Activity activity;
    private MyAdapter myAdapter;

    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");//ISO-8859-1
    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        PermisionUtils.verifyStoragePermissions(this);
        recycler = findViewById(R.id.recycler);
        myAdapter = new MyAdapter(activity);
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(myAdapter);

        initData();
    }

    private void initData() {
        if (!PermisionUtils.haveStoragePermissions(activity)) {
            return;
        }
        // 为 UserBean 的列表创建一个 数据库对象
        unqliteArray = new UnqliteArray();
        executorService.execute(() -> {
            unqliteArray.open(Objects.requireNonNull(activity.getExternalFilesDir("database")).getAbsolutePath() + File.separator + "Array100w.db");

            myAdapter.setUnqliteArray(unqliteArray);
            long time = System.currentTimeMillis();
            //恢复保存的数据, 需要保存的话,切勿调用clear()方法
            unqliteArray.restore();
            // 这里测试100w条数据, 也可以去掉判断往后面添加数据.
            if (unqliteArray.size() <= 0) {
                for (int i = 0; i < TestCount; i++) {
                    UserBean userBean = new UserBean();
                    userBean.setName((i + 1) + ",编号:" + (i));
                    userBean.setAge(25 + i % 10);
                    unqliteArray.add(userBean);
                    if (i % 10000 == 0) {
                        runOnUiThread(() -> Objects.requireNonNull(myAdapter).notifyDataChanged());
                    }
                }
            }
            //保存,下次读取
            unqliteArray.save();
            Log.i("unqlite", "插入 " + TestCount + " 条数据,用时: " + (System.currentTimeMillis() - time) + " ms");
            runOnUiThread(() -> {
                int position = unqliteArray.size() / 2;
                Objects.requireNonNull(myAdapter).notifyDataChanged();
                recycler.scrollToPosition(position);
                LinearLayoutManager mLayoutManager =
                        (LinearLayoutManager) recycler.getLayoutManager();
                if (mLayoutManager != null) {
                    mLayoutManager.scrollToPositionWithOffset(position, 0);
                }

            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            initData();

        } else {

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            PermisionUtils.verifyStoragePermissions(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unqliteArray != null) {
            //清理数据,需要保存得话就不调用
//            unqliteArray.clear();
            // 关闭
            unqliteArray.close();
        }
    }

    public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        private final LayoutInflater layoutInflater;
        private UnqliteArray unqliteArray;
        AtomicInteger count = new AtomicInteger(0);

        MyAdapter(Activity activity) {
            layoutInflater = LayoutInflater.from(activity);
        }

        void setUnqliteArray(UnqliteArray unqliteArray) {
            this.unqliteArray = unqliteArray;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            @SuppressLint("InflateParams")
            View inflate = layoutInflater.inflate(R.layout.list_user, null);
            return new MyViewHolder(inflate);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
            long time = System.nanoTime();
            UserBean userBean = unqliteArray.get(i);
            Log.i("unqlite", "加载用时:" + ((System.nanoTime() - time) / 1000) + "微秒");
            if (userBean != null) {
                myViewHolder.nameText.setText(userBean.getName());
                myViewHolder.ageText.setText(userBean.getAge() + "");
            } else {
                myViewHolder.nameText.setText("null");
            }

        }

        @Override
        public int getItemCount() {
            return count.get();
        }

        public void notifyDataChanged() {
            if (unqliteArray != null) {
                int c = count.get();
                int size = unqliteArray.size();
                count.set(size);
                if (c < size) {
                    this.notifyItemRangeChanged(c, size - c);
                } else if (c > size) {
                    this.notifyDataSetChanged();
                }
            }
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView ageText;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.name);
            ageText = itemView.findViewById(R.id.age);
        }
    }

}
