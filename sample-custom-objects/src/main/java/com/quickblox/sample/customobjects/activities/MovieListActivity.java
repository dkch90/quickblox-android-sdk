package com.quickblox.sample.customobjects.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.customobjects.QBCustomObjects;
import com.quickblox.customobjects.model.QBCustomObject;
import com.quickblox.sample.customobjects.R;
import com.quickblox.sample.customobjects.adapter.MovieListAdapter;
import com.quickblox.sample.customobjects.utils.Consts;
import com.quickblox.sample.customobjects.helper.DataHolder;
import com.quickblox.sample.customobjects.model.Movie;

import java.util.ArrayList;
import java.util.Map;

public class MovieListActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    private static final String createDateField = "created_at";

    private MovieListAdapter movieListAdapter;
    private SwipyRefreshLayout setOnRefreshListener;
    private QBRequestGetBuilder builder;
    private int skipRecords = 0;

    public static void start(Context context) {
        Intent intent = new Intent(context, MovieListActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movies_list);
        DataHolderClear();
        initUI();
        getMovieList(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        movieListAdapter.updateData(DataHolder.getInstance().getMovieMap());
    }

    private void DataHolderClear() {
        if (!DataHolder.getInstance().getMovieMap().isEmpty()) {
            DataHolder.getInstance().clear();
        }
    }

    private void initUI() {
        ListView moviesListView = _findViewById(R.id.list_movies);
        moviesListView.setOnItemClickListener(this);
        movieListAdapter = new MovieListAdapter(this, DataHolder.getInstance().getMovieMap());
        moviesListView.setAdapter(movieListAdapter);

        builder = new QBRequestGetBuilder();
        setOnRefreshListener = _findViewById(R.id.swipy_refresh_layout);
        setOnRefreshListener.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                builder.setSkip(skipRecords += Consts.LIMIT_RECORDS);
                getMovieList(false);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Movie movie = (Movie) adapterView.getItemAtPosition(position);
        ShowMovieActivity.start(this, movie.getId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_movies_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_movie:
                AddNewMovieActivity.start(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getMovieList(boolean progress) {
        if (progress) {
            progressDialog.show();
        }
        builder.setLimit(Consts.LIMIT_RECORDS);
        builder.sortDesc(createDateField);
        QBCustomObjects.getObjects(Consts.CLASS_NAME, builder, new QBEntityCallback<ArrayList<QBCustomObject>>() {
            @Override
            public void onSuccess(ArrayList<QBCustomObject> qbCustomObjects, Bundle bundle) {
                setOnRefreshListener.setEnabled(true);
                Map<String, Movie> movieMap = DataHolder.getInstance().getMovieMap();

                for (QBCustomObject customObject : qbCustomObjects) {
                    DataHolder.getInstance().addMovieToMap(new Movie(customObject));
                }

                progressDialog.dismiss();
                setOnRefreshListener.setRefreshing(false);
                movieListAdapter.updateData(movieMap);
            }

            @Override
            public void onError(QBResponseException e) {
                setOnRefreshListener.setEnabled(false);
                View rootLayout = findViewById(R.id.swipy_refresh_layout);
                showSnackbarError(rootLayout, R.string.splash_create_session_error, e, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getMovieList(false);
                    }
                });
                progressDialog.dismiss();
                setOnRefreshListener.setRefreshing(false);
            }
        });
    }
}