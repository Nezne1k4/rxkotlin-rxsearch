/*
 * Copyright (c) 2017 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.yahami.searcher

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.gson.Gson
import com.yahami.searcher.data.Country
import com.yahami.searcher.search_feature.SearchAdapter
import com.yahami.searcher.search_feature.SearchEngine
import io.reactivex.Observable
import io.reactivex.Observable.create
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search.*
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Reference: https://www.raywenderlich.com/170233/reactive-programming-rxandroid-kotlin-introduction
 *
 * Introduction to Rx with Kotlin:
 * - define an observable (create with emitter)
 * - turn asynchronous events like button clicks and text field context changes into observables
 * - transform observable items (map, ...)
 * - filter observable items (filter)
 * - reduce backpressure with debounce(time)
 * - combine several observables into one (merge, concat)
 * - specify the thread on which code should be executed
 */

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEngine: SearchEngine
    private val searchAdapter = SearchAdapter()

    private var searchDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchResults.layoutManager = LinearLayoutManager(this)
        searchResults.adapter = searchAdapter

        // init the searchEngine
        try {
            var countries: Array<String> = emptyArray()
            val json = application.assets.open("countries.json").bufferedReader().use {
                it.readText()
            }
            json?.apply {
                countries = Gson().fromJson(json, Array<Country>::class.java)
                        .map {
                            String.format("${it.value} - ${it.label}")
                        }.toTypedArray()
            }
            searchEngine = SearchEngine(countries)
        } catch (e: FileNotFoundException) {
            searchEngine = SearchEngine(resources.getStringArray(R.array.cheeses))
            Toast.makeText(this, "Not found countries.json file, use cheeses instead", Toast.LENGTH_LONG).show()
        }
    }

    protected fun showProgress() {
        progressBar.visibility = VISIBLE
    }

    protected fun hideProgress() {
        progressBar.visibility = GONE
    }

    protected fun showResult(result: List<String>) {
        if (result.isEmpty()) {
            Toast.makeText(this, R.string.nothing_found, Toast.LENGTH_SHORT).show()
        }
        searchAdapter.cheeses = result
    }

    // 1. create Search button observe
    fun createSearchButtonObservableLong(): Observable<String> {
        return create { emitter: ObservableEmitter<String> ->
            searchButton.setOnClickListener(View.OnClickListener { _: View? ->
                emitter.onNext(queryEditText.text.toString().trim())
            })

            /**
             * Keeping references can cause memory leaks in Java or Kotlin.
             * It’s a useful habit to remove listeners as soon as they are no longer needed.
             * But what do you call when you are creating your own Observable?
             * For that very reason, ObservableEmitter has setCancellable().
             * Override cancel(), and your implementation will be called when the Observable is disposed,
             * such as when the Observable is completed or all Observers have unsubscribed from it.
             */
            emitter.setCancellable(object : Cancellable {
                override fun cancel() {
                    searchButton.setOnClickListener(null)
                }
            })
        }
    }

    // shorter way to code
    fun createSearchButtonObservable(): Observable<String> {
        return create<String> { emitter ->
            searchButton.setOnClickListener {
                //_ -> emitter.onNext(queryEditText.text.toString())
                emitter.onNext(queryEditText.text.toString())
            }

            // override cancel() when emitter cancels
            emitter.setCancellable {
                searchButton.setOnClickListener(null)
            }
        }
                .filter { it.isNotEmpty() }
                .debounce(1, TimeUnit.SECONDS)
    }

    fun createTextChangeObservable(): Observable<String> {
        val observable = create<String> { emitter ->
            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // do nothing
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    //Log.d("Search", "onTextChanged")
                    s?.toString()?.let {
                        emitter.onNext(it.trim())
                    }
                }
            }

            queryEditText.addTextChangedListener(textWatcher)

            // for imeActionSearch on keyboard
            queryEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    queryEditText?.text.toString().let {
                        emitter.onNext(it.trim())
                    }
                    true
                } else {
                    false
                }
            }

            emitter.setCancellable {
                queryEditText.removeTextChangedListener(textWatcher)
                queryEditText.setOnEditorActionListener(null)
            }
        }

        return observable
                //.filter { it.length >= 1 } // move checking condition to searchEngine.search()
                .debounce(1, TimeUnit.SECONDS)
                .distinctUntilChanged() // notify only when text changed from the last one
    }

    override fun onStart() {
        super.onStart()
        Log.d("Search", "onStart")
        // stream
        val searchButtonStream = createSearchButtonObservable()
        val textChangeStream = createTextChangeObservable()

        /**
         * merge the sources
         * whatever comes first, it will emit
         */
        val searchObservable = Observable.merge<String>(searchButtonStream, textChangeStream)

        searchDisposable = searchObservable
                // code that works with Views should execute on the main thread
                .subscribeOn(AndroidSchedulers.mainThread())
                // because of doOnNext{ } is in main thread, must change thread here
                // else will crash
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { showProgress() }
                // move to io() for mapping
                .observeOn(Schedulers.io())
                .flatMap { string -> Observable.fromIterable(string.split(" ")) }
                .map {
                    //it -> searchEngine.search(it)
                    Log.d("Search", "Search for $it ${Date().time / 1000}")
                    searchEngine.search(it)
                }
                // back to main thread to show result in subscribe { }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideProgress()
                    showResult(it)
                }
    }

    override fun onStop() {
        super.onStop()
        Log.d("Search", "onStop")

        // dispose to prevent leak
        searchDisposable?.apply {
            if (!this.isDisposed) {
                Log.d("Search", "searchDisposable is disposed")
                this.dispose()
            }
        }
    }

}