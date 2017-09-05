package pl.hypeapp.presentation.seasons

import pl.hypeapp.domain.model.AllSeasonsModel
import pl.hypeapp.domain.model.EpisodeModel
import pl.hypeapp.domain.model.SeasonModel
import pl.hypeapp.domain.model.WatchState
import pl.hypeapp.domain.usecase.base.DefaultCompletableObserver
import pl.hypeapp.domain.usecase.base.DefaultSingleObserver
import pl.hypeapp.domain.usecase.mapwatched.SeasonWatchStateIntegrityUseCase
import pl.hypeapp.domain.usecase.seasons.AllEpisodesUseCase
import pl.hypeapp.domain.usecase.watchstate.ManageEpisodeWatchStateUseCase
import pl.hypeapp.domain.usecase.watchstate.ManageSeasonsWatchStateUseCase
import pl.hypeapp.presentation.base.Presenter
import java.util.logging.Logger
import javax.inject.Inject

class SeasonsPresenter @Inject constructor(private val allEpisodesUseCase: AllEpisodesUseCase,
                                           private val manageEpisodeWatchStateUseCase: ManageEpisodeWatchStateUseCase,
                                           private val manageSeasonWatchStateUseCase: ManageSeasonsWatchStateUseCase,
                                           private val seasonWatchStateIntegrityUseCase: SeasonWatchStateIntegrityUseCase)
    : Presenter<SeasonsView>() {

    var isViewShown = false

    override fun onAttachView(view: SeasonsView) {
        super.onAttachView(view)
        this.view?.initRecyclerAdapter()
        this.view?.initSwipeToRefresh()
        this.view?.showLoading()
        this.view?.observeWatchStateInParentActivity()
    }

    override fun onDetachView() {
        super.onDetachView()
        allEpisodesUseCase.dispose()
        manageEpisodeWatchStateUseCase.dispose()
        manageSeasonWatchStateUseCase.dispose()
    }

    // For performance reason load list when a fragment is visible for first time.
    fun onViewShown() {
        if (!isViewShown) {
            isViewShown = true
            this.view?.loadViewModel()
        }
    }

    fun requestAllSeasons(tvShowId: String, update: Boolean) {
        this.view?.showLoading()
        allEpisodesUseCase.execute(SeasonsObserver(), AllEpisodesUseCase.Params.createQuery(tvShowId, update))
    }

    fun changeEpisodeWatchState(episodeModel: EpisodeModel) = with(episodeModel) {
        val addToWatched: Boolean = watchState != WatchState.WATCHED
        watchState = WatchState.manageWatchState(watchState)
        manageEpisodeWatchStateUseCase.execute(ManageWatchStateObserver(),
                ManageEpisodeWatchStateUseCase.Params.createParams(this, addToWatched))
    }

    fun changeSeasonWatchState(seasonModel: SeasonModel) = with(seasonModel) {
        val addToWatched: Boolean = watchState != WatchState.WATCHED
        watchState = WatchState.manageWatchState(watchState)
        manageSeasonWatchStateUseCase.execute(ManageWatchStateObserver(),
                ManageSeasonsWatchStateUseCase.Params.createParams(this, addToWatched))
    }

    fun checkWatchStateIntegrity(allSeasonsModel: AllSeasonsModel?) {
        if (isViewShown) {
            seasonWatchStateIntegrityUseCase.execute(UpdateSeasonsObserver(),
                    SeasonWatchStateIntegrityUseCase.Params.createParams(allSeasonsModel!!))
        }
    }

    val log: Logger = Logger.getLogger("Seasons")

    inner class SeasonsObserver : DefaultSingleObserver<AllSeasonsModel>() {
        override fun onSuccess(model: AllSeasonsModel) {
            if (model.seasons?.size != 0) {
                this@SeasonsPresenter.view?.populateRecyclerView(model)
            } else {
                this@SeasonsPresenter.view?.showEmptySeasonsMessage()
            }
        }

        override fun onError(error: Throwable) {
            this@SeasonsPresenter.view?.showError()
        }
    }

    inner class ManageWatchStateObserver : DefaultCompletableObserver() {
        override fun onComplete() {
            this@SeasonsPresenter.view?.onChangedWatchState()
        }

        override fun onError(e: Throwable) {
            this@SeasonsPresenter.view?.onChangeWatchStateError()
        }
    }

    inner class UpdateSeasonsObserver : DefaultSingleObserver<AllSeasonsModel>() {
        override fun onSuccess(model: AllSeasonsModel) {
            this@SeasonsPresenter.view?.updateRecyclerList(model)
        }
    }

}