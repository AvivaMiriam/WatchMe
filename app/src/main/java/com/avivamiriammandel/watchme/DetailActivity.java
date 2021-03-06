package com.avivamiriammandel.watchme;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.avivamiriammandel.watchme.adapter.ReviewsAdapter;
import com.avivamiriammandel.watchme.adapter.TrailersAdapter;

import com.avivamiriammandel.watchme.database.AppDatabase;
import com.avivamiriammandel.watchme.error.ApiError;
import com.avivamiriammandel.watchme.error.ReviewErrorUtils;
import com.avivamiriammandel.watchme.error.TrailerErrorUtils;
import com.avivamiriammandel.watchme.glide.GlideApp;
import com.avivamiriammandel.watchme.model.Movie;
import com.avivamiriammandel.watchme.model.Review;
import com.avivamiriammandel.watchme.model.ReviewsResponse;
import com.avivamiriammandel.watchme.model.Trailer;
import com.avivamiriammandel.watchme.model.TrailersResponse;
import com.avivamiriammandel.watchme.rest.Client;
import com.avivamiriammandel.watchme.rest.Service;
import com.avivamiriammandel.watchme.viewmodel.AppExecutors;
import com.avivamiriammandel.watchme.viewmodel.MovieViewModel;
import com.avivamiriammandel.watchme.viewmodel.MovieViewModelFactory;
import com.github.florent37.glidepalette.GlidePalette;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.materialratingbar.MaterialRatingBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DetailActivity extends AppCompatActivity {

    private static final String TAG = DetailActivity.class.getSimpleName();
    AppDatabase db;
    Context context;
    Movie movie, favoriteMovie;
    android.support.v4.widget.NestedScrollView detailScrollView, detailScrollViewRecycler,
            detailScrollViewRecyclerReview;
    TextView plotSynopsis, userRating, releaseDate, trailersTitle, reviewsTitle;
    ImageView backdropView;
    MaterialRatingBar ratingStars;
    RecyclerView recyclerView, recyclerViewReview;
    TrailersAdapter adapter;
    List<Trailer> trailerList;
    List<Review> reviewList;
    CardView cardView;
    Toolbar toolbar;
    BottomNavigationView navigation;
    ConstraintLayout constraintLayoutDetails, constraintLayoutRecycler,
            constraintLayoutRecyclerReview;
    Boolean recyclerNull = false, navDetail, navTrailer, navReview,
            isMovie,  justAdd = false, justDelete = false;
    float dpWidth;
    float dpHeight;

    private AppCompatActivity activity = DetailActivity.this;
    MaterialFavoriteButton materialFavoriteButton;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);




        context = getApplicationContext();

        navigation = findViewById(R.id.navigation_detail);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        toolbar = findViewById(R.id.toolbar);




        detailScrollView = findViewById(R.id.detail_scroll_view_movie_details);
        detailScrollViewRecycler = findViewById(R.id.detail_scroll_view_movie_recycler);
        detailScrollViewRecyclerReview = findViewById(R.id.detail_scroll_view_movie_recycler_review);
        backdropView = findViewById(R.id.backdrop_image_view);

        trailersTitle = findViewById(R.id.text_view_trailer_title);
        reviewsTitle = findViewById(R.id.text_view_review_title);
        plotSynopsis = findViewById(R.id.text_view_plot_synopsis);
        userRating = findViewById(R.id.text_view_user_rating);
        ratingStars = findViewById(R.id.rating_bar);
        releaseDate = findViewById(R.id.text_view_release_date);
        constraintLayoutDetails = findViewById(R.id.constraint_layout_movie_details);
        constraintLayoutRecycler = findViewById(R.id.constraint_layout_movie_recycler);
        constraintLayoutRecyclerReview = findViewById(R.id.constraint_layout_movie_recycler_review);
        materialFavoriteButton = findViewById(R.id.button_favorite);


        final Intent intent = getIntent();
        if (intent.hasExtra(context.getString(R.string.movies_parcelable_object))) {

            navDetail = true;
            navReview = false;
            navTrailer = false;
            detailScrollView.setVisibility(View.VISIBLE);
            detailScrollView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            detailScrollViewRecycler.setVisibility(View.INVISIBLE);
            detailScrollViewRecycler.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
            detailScrollViewRecyclerReview.setVisibility(View.INVISIBLE);
            detailScrollViewRecyclerReview.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));

            fillViews();

            setMaterialFavoriteButtonOnLoad();






                    materialFavoriteButton.setOnFavoriteChangeListener(
                    new MaterialFavoriteButton.OnFavoriteChangeListener() {
                        @Override
                        public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                                     if (favorite) {
                                        if (favoriteMovie == null) {
                                            if (!justDelete) {
                                                saveFavorite();
                                                Snackbar.make(buttonView, getString(R.string.title_added_to_favorites),
                                                        Snackbar.LENGTH_LONG).show();
                                                justAdd = true;
                                            } else {
                                                justDelete = false;
                                            }
                                        }else
                                            Snackbar.make(buttonView, getString(R.string.title_already_added_to_favorites),
                                                    Snackbar.LENGTH_LONG).show();

                                    } else {
                                        if (favoriteMovie != null) {
                                            if (!justAdd) {
                                                Snackbar.make(buttonView, getString(R.string.title_removed_from_favorites),
                                                        Snackbar.LENGTH_LONG).show();
                                                deleteFavorite();
                                                justDelete = true;
                                            } else
                                                justAdd = false;
                                        } else
                                            Snackbar.make(buttonView, getString(R.string.title_already_removed_from_favorites),
                                                    Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            });



        }else {
            Toast.makeText(context, getString(R.string.no_api_data), Toast.LENGTH_SHORT).show();
        }

    }

    @NonNull
    private void setMaterialFavoriteButtonOnLoad() {
        db = AppDatabase.getInstance(getApplicationContext());

        MovieViewModelFactory modelFactory = new MovieViewModelFactory(db, movie.getId());
        final MovieViewModel movieViewModel = ViewModelProviders.of(this, modelFactory).get(MovieViewModel.class);
        movieViewModel.getMovie().observe(DetailActivity.this, new Observer<Movie>() {
            @Override
            public void onChanged(@Nullable Movie movie) {
                favoriteMovie = movie;
                if (movie == null) {
                    materialFavoriteButton.setFavorite(false);
                    Log.d(TAG, "onChanged: favorite not found");
                    // deselect the favorite button
                } else {
                    materialFavoriteButton.setFavorite(true);
                    Log.d(TAG, "onChanged: favorite found");
                    // select the button
                }
            }
        });

    }

    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_list:
                    navDetail = false;
                    navReview = false;
                    navTrailer = true;
                    setNavigationListVisibility();
                    return true;
                case R.id.navigation_trailer:
                    navDetail = false;
                    navReview = false;
                    navTrailer = true;
                    setTrailerListVisibility();
                    return true;
                case R.id.navigation_review:
                    navDetail = false;
                    navReview = true;
                    navTrailer = false;
                    setReviewListVisibility();
                    return true;
                default:
                    return false;
            }
        }
    };

    private void setReviewListVisibility() {
        if (isOnline()) {
            loadJSON1();
            detailScrollView.setVisibility(View.INVISIBLE);
            detailScrollView.setLayoutParams(new ConstraintLayout.LayoutParams(0,0));
            detailScrollViewRecycler.setVisibility(View.INVISIBLE);
            detailScrollViewRecycler.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
            detailScrollViewRecyclerReview.setVisibility(View.VISIBLE);
            detailScrollViewRecyclerReview.setLayoutParams(new ConstraintLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (recyclerNull) {
                String noReviews = getString(R.string.title_reviews) + getString(R.string.no_reviews);
                reviewsTitle.setText(noReviews);
            } else {
                reviewsTitle.setText(R.string.title_reviews);
            }


            }
        else {
            // Not Available...
            Toast.makeText(context, R.string.onNoInternetError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setTrailerListVisibility() {
        if (isOnline()) {

        // Its Available...
            detailScrollView.setVisibility(View.INVISIBLE);
            detailScrollView.setLayoutParams(new ConstraintLayout.LayoutParams(0,0));
            detailScrollViewRecycler.setVisibility(View.VISIBLE);
            detailScrollViewRecycler.setLayoutParams(new ConstraintLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            detailScrollViewRecyclerReview.setVisibility(View.INVISIBLE);
            detailScrollViewRecyclerReview.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
            trailersTitle.setText(R.string.title_trailers);
            loadJSON();
            } else {
            // Not Available...
            Toast.makeText(context, R.string.onNoInternetError, Toast.LENGTH_LONG).show();
            finish();

        }
    }

    private void setNavigationListVisibility() {
        detailScrollView.setVisibility(View.VISIBLE);
        detailScrollView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        detailScrollViewRecycler.setVisibility(View.INVISIBLE);
        detailScrollViewRecycler.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
        detailScrollViewRecyclerReview.setVisibility(View.INVISIBLE);
        detailScrollViewRecyclerReview.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
        navDetail = true;
        navReview = false;
        navTrailer = false;
    }

    private void fillViews() {

        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        dpHeight = displayMetrics.heightPixels / displayMetrics.density;

        movie = getIntent().getParcelableExtra(context.getString(R.string.movies_parcelable_object));

            final String thumbnailUrl = movie.getPosterPath();
        final String backdropUrl = movie.getBackdropPath();

        final String movieTitle = movie.getTitle();
        toolbar.setTitle(movieTitle);
        final String synopsis = movie.getOverview();

        final Double voteDoubleSpare = movie.getVoteAverage();
        final DecimalFormat format = new DecimalFormat("##.0");
        final String vote = (format.format(voteDoubleSpare));
        final String ratingOutOfTen = vote + " /" + " 10";


        final Double voteInHalf = movie.getVoteAverage() / 2;
        final DecimalFormat format1 = new DecimalFormat("##.0");
        final String voteHalved = (format1.format(voteInHalf));
        ratingStars.setRating(Float.valueOf(voteHalved));


        final String release = movie.getReleaseDate();

        plotSynopsis.setText(synopsis);
        userRating.setText(ratingOutOfTen);
        releaseDate.setText(release);
        int error;
        if (dpWidth >= dpHeight)
            error = R.drawable.the_movie_db_error_loading_poster_land ;
        else
            error = R.drawable.the_movie_db_error_loading_poster ;


        try {
            GlideApp.with(context)
                    .load(thumbnailUrl)
                    .listener(GlidePalette.with(thumbnailUrl)
                            .use(GlidePalette.Profile.MUTED)
                            .intoBackground(detailScrollView)
                            .crossfade(true)

                            .use(GlidePalette.Profile.VIBRANT_DARK)
                            .intoTextColor(plotSynopsis)
                            .intoTextColor(userRating)
                            .intoTextColor(releaseDate)
                            .crossfade(true)
                    )
                    .placeholder(R.drawable.the_movie_db_loading_poster)
                    .error(error)
                    .into(backdropView);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, getString(R.string.on_bind_view_holder) + e.getMessage());
        }

        try {
            GlideApp.with(context)
                    .load(backdropUrl)
                    .listener(GlidePalette.with(backdropUrl)
                            .use(GlidePalette.Profile.MUTED)
                            .intoBackground(toolbar)
                            .crossfade(true)

                    )

                    .placeholder(R.drawable.the_movie_db_loading_poster)
                    .error(error)
                    .into(backdropView);
        } catch (final IllegalArgumentException e) {
            Log.e(TAG, getString(R.string.on_bind_view_holder) + e.getMessage());
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setElevation(10.f);
    }

    private void initViews(){
        trailerList = new ArrayList<>();
        adapter = new TrailersAdapter(this, trailerList);


        if (isOnline()) {
            // Its Available...
            loadJSON();
        } else {
            // Not Available...
            Toast.makeText(context, R.string.onNoInternetError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadJSON() {
        int movieId = movie.getId();
        try {
            if (BuildConfig.API_KEY.isEmpty()) {
                Toast.makeText(getApplicationContext(), R.string.apiKeyError, Toast.LENGTH_LONG).show();
                return;
            }

            Client client = new Client();
            Service apiService = Client.getClient().create(Service.class);
            Call<TrailersResponse> call = apiService.getMovieTrailers(movieId, BuildConfig.API_KEY);
            call.enqueue(new Callback<TrailersResponse>() {
                @Override
                public void onResponse(@NonNull Call<TrailersResponse> call, @NonNull Response<TrailersResponse> response) {
                    if (response.isSuccessful()) {
                        TrailersResponse results = response.body();
                        trailerList = null;
                        if (results != null) {
                            recyclerView = findViewById(R.id.recycler_view_trailer_details);
                            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                            recyclerView.setLayoutManager(layoutManager);
                            trailerList = results.getResults();
                            recyclerView.setAdapter(new TrailersAdapter(getApplicationContext(), trailerList));
                            recyclerView.smoothScrollToPosition(0);
                        } else {

                            ApiError apiError = TrailerErrorUtils.parseError(response);
                            Toast.makeText(getApplicationContext(), apiError.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, getString(R.string.on_response) + apiError.getMessage() + " " + apiError.getStatusCode() + " " + apiError.getEndpoint());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TrailersResponse> call, @NonNull Throwable t) {
                    Toast.makeText(getApplicationContext(), R.string.onNoInternetError, Toast.LENGTH_LONG).show();
                    Log.d(TAG, getString(R.string.on_failure) + t.getMessage());
                }
            });
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG, R.string.on_exception + e.getMessage());
        }
    }


    private void loadJSON1() {
        int movieId = movie.getId();
        try {
            if (BuildConfig.API_KEY.isEmpty()) {
                Toast.makeText(getApplicationContext(), R.string.apiKeyError, Toast.LENGTH_LONG).show();
                return;
            }

            Client client = new Client();
            Service apiService = Client.getClient().create(Service.class);
            Call<ReviewsResponse> call = apiService.getMovieReviews(movieId, BuildConfig.API_KEY);
            call.enqueue(new Callback<ReviewsResponse>() {
            @Override
                public void onResponse(@NonNull Call<ReviewsResponse> call, @NonNull Response<ReviewsResponse> response) {
                    if (response.isSuccessful()) {
                        ReviewsResponse results = response.body();
                        reviewList = null;

                        if (results != null) {
                            recyclerViewReview = findViewById(R.id.recycler_view_review_details);
                            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                            recyclerViewReview.setLayoutManager(layoutManager);

                            reviewList = results.getResults();
                            recyclerNull = false;
                            if (reviewList.size() == 0) {
                                recyclerNull = true;
                            } else {
                                recyclerViewReview.setAdapter(new ReviewsAdapter(context, reviewList));
                                recyclerViewReview.smoothScrollToPosition(0);
                            }
                        } else {

                            ApiError apiError = ReviewErrorUtils.parseError(response);
                            Toast.makeText(getApplicationContext(), apiError.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, R.string.on_response + apiError.getMessage() + " " + apiError.getStatusCode() + " " + apiError.getEndpoint());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ReviewsResponse> call, @NonNull Throwable t) {
                    Toast.makeText(getApplicationContext(), R.string.onNoInternetError, Toast.LENGTH_LONG).show();
                    Log.d(TAG, R.string.on_failure + t.getMessage());
                }
            });
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG, R.string.on_exception + e.getMessage());
        }
    }

    public void saveFavorite() {
        final Movie favoriteMovie = new Movie(
                movie.getId(),
                movie.getVoteAverage(),
                movie.getTitle(),
                movie.getPosterPath(),
                movie.getBackdropPath(),
                movie.getOverview(),
                movie.getReleaseDate()
                
        );

        final AppDatabase database = AppDatabase.getInstance(context);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                //if (!(movie.getId().equals(database.movieDao().loadMovieById(movie.getId()).getValue().getId()))) {
                    database.movieDao().insertMovie(favoriteMovie);
                //}
            }
        });
    }

    private void deleteFavorite() {
        final Movie favoriteMovie = new Movie(
                movie.getId(),
                movie.getVoteAverage(),
                movie.getTitle(),
                movie.getPosterPath(),
                movie.getBackdropPath(),
                movie.getOverview(),
                movie.getReleaseDate()
        );
        final AppDatabase database = AppDatabase.getInstance(context);

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                //if (movie.getId().equals(database.movieDao().loadMovieById(movie.getId()).getValue().getId())) {
                    database.movieDao().deleteMovie(favoriteMovie);
                //}
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isMovie = false;
        db = AppDatabase.getInstance(getApplicationContext());


        MovieViewModelFactory modelFactory = new MovieViewModelFactory(db, movie.getId());
        final MovieViewModel movieViewModel2 = ViewModelProviders.of(this, modelFactory).get(MovieViewModel.class);
        movieViewModel2.getMovie().observe(DetailActivity.this, new Observer<Movie>() {
                    @Override
                    public void onChanged(@Nullable Movie movie) {
                        if (movie != null) {
                            movieViewModel2.getMovie().removeObserver(this);
                            isMovie = true;
                            DetailActivity.this.movie = movie;

                        }
                    }
                });
        Intent intent = getIntent();
        if (intent.hasExtra(context.getString(R.string.movies_parcelable_object)))
            isMovie = true;
            movie =  getIntent().getParcelableExtra(context.getString(R.string.movies_parcelable_object));

        if (isMovie) {
            outState.putParcelable(getString(R.string.movies_parcelable_object), DetailActivity.this.movie);
            outState.putBoolean(getString(R.string.movie_details), navDetail);
            outState.putBoolean(getString(R.string.title_trailer), navTrailer);
            outState.putBoolean(getString(R.string.title_review), navReview);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null){
            movie = savedInstanceState.getParcelable(context.getString(R.string.movies_parcelable_object));
            if (savedInstanceState.getBoolean(context.getString(R.string.title_trailer))){
                Log.d(TAG, "onRestoreInstanceState: get Trailer");
                fillViews();
                initViews();
                setTrailerListVisibility();
                navigation.setSelectedItemId(R.id.navigation_trailer);
            } else if (savedInstanceState.getBoolean(context.getString(R.string.title_review))) {
                Log.d(TAG, "onRestoreInstanceState: get Review");
                fillViews();
                loadJSON1();
                setReviewListVisibility();
                navigation.setSelectedItemId(R.id.navigation_review);
            } else if (savedInstanceState.getBoolean(context.getString(R.string.movie_details))){
                Log.d(TAG, "onRestoreInstanceState: get Details");
                fillViews();
                setNavigationListVisibility();
                setMaterialFavoriteButtonOnLoad();
                navigation.setSelectedItemId(R.id.navigation_detail);
            }
        }
    }

    public boolean isOnline() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}


