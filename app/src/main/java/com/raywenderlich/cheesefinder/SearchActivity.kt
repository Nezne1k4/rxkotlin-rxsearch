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

package com.raywenderlich.cheesefinder

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Cancellable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*
import java.util.concurrent.TimeUnit

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEngine: SearchEngine
    private val searchAdapter = SearchAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchResults.layoutManager = LinearLayoutManager(this)
        searchResults.adapter = searchAdapter

        // init the searchEngine
        searchEngine = SearchEngine(resources.getStringArray(R.array.cheeses))


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
        return Observable.create { emitter: ObservableEmitter<String> ->
            searchButton.setOnClickListener(View.OnClickListener { _: View? ->
                emitter.onNext(queryEditText.text.toString())
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
        return Observable.create<String> { emitter ->
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
        val observable = Observable.create<String> { emitter ->
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
                        emitter.onNext(it)
                    }
                }
            }

            queryEditText.addTextChangedListener(textWatcher)

            emitter.setCancellable {
                queryEditText.removeTextChangedListener(textWatcher)
            }
        }

        return observable
                .filter { it.length >=1 }
                .debounce(1, TimeUnit.SECONDS)
    }

    private var searchObservableDisposable: Disposable? = null

    override fun onStart() {
        super.onStart()
        Log.d("Search", "onStart")
        // subcription
        val searchButtonStream = createSearchButtonObservable()
        val textChangeStream = createTextChangeObservable()

        // whatever comes first, it will emit
        val searchObservable = Observable.merge<String>(searchButtonStream, textChangeStream)

        searchObservableDisposable = searchObservable
                // code that works with Views should execute on the main thread
                .subscribeOn(AndroidSchedulers.mainThread())
                // because of doOnNext{ } is in main thread, must change thread here
                // else will crash
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { showProgress() }
                // move to io() for mapping
                .observeOn(Schedulers.io())
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

    override fun onResume() {
        super.onResume()
        Log.d("Search", "onResume")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Search", "onStop")
        searchObservableDisposable?.apply {
            if (!this.isDisposed) {
                Log.d("Search", "searchObservableDisposable is disposed")
                this.dispose()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("Search", "onPause")
    }

}