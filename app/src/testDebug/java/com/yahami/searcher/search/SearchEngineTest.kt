@file:Suppress("IllegalIdentifier")

package com.yahami.searcher.search;

import com.google.gson.Gson
import com.yahami.searcher.data.Country
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchEngineTest {
    lateinit var searchEngine: SearchEngine

    @Before
    fun setUp() {
        var countries: Array<String> = emptyArray()
        // load data from 'resources'
        val json = javaClass.classLoader.getResourceAsStream(("countries.json")).bufferedReader().use {
            it.readText()
        }
        json?.apply {
            countries = Gson().fromJson(json, Array<Country>::class.java)
                    .map {
                        String.format("${it.value} - ${it.label}")
                    }.toTypedArray()
        }
        searchEngine = SearchEngine(countries).also {
            assertEquals(243, it.datalist.size)
        }

    }

    @Test
    fun `Empty or blank string return no data`() {
        val blanks = arrayOf("", " ", "   ", "     ")
        blanks.forEach {
            assertEquals(emptyList<String>(), searchEngine.search(it))
        }
    }

    @Test
    fun `Plus only cases return no data`() {
        val blanks = arrayOf("+", " +", " +", " + ", "  +  ")
        blanks.forEach {
            assertEquals(emptyList<String>(), searchEngine.search(it))
        }
    }

    @Test
    fun `Spaces are all trim`() {
        val blanks = arrayOf("thai", " thai", "thai ", " thai ", "  thai  ")
        blanks.forEach {
            assertEquals(listOf("TH - Thailand"), searchEngine.search(it))
        }
    }

    @Test
    fun `Spaces are all trim 2`() {
        val blanks = arrayOf("thai viet", " thai  viet", "thai  viet", " thai  viet", "  thai  viet   ")
        blanks.forEach {
            assertEquals(listOf("TH - Thailand", "VN - Vietnam"), searchEngine.search(it))
        }
    }

    @Test
    fun `Order makes sense 1`() {
        " viet  kingdom  united".also {
            searchEngine.search(it).apply {
                assertTrue(this.size > 1)
                assertEquals("VN - Vietnam", this.first())
            }
        }
    }

    @Test
    fun `Order makes sense 2`() {
        " kingdom  united  viet".also {
            searchEngine.search(it).apply {
                assertTrue(this.size > 1)
                assertEquals("VN - Vietnam", this.last())
            }
        }
    }

    @Test
    fun `No duplicated elements added`() {
        " united viet kingdom".also {
            searchEngine.search(it).apply {
                assertTrue(this.size > 1)
                assertEquals("VN - Vietnam", this.last())
            }
        }
    }

    @Test
    fun `+ combines only duplicated, results returns value 1`() {
        val blanks = arrayOf("united+kingdom", " united + kingdom ", "  united  +  kingdom +  ")
        blanks.forEach {
            assertEquals(listOf("GB - United Kingdom"), searchEngine.search(it))
        }
        "united+kingdom".apply {

        }
    }

    @Test
    fun `+ combines only duplicated, results returns value 2`() {
        "+united+kingdom".apply {
            assertEquals(listOf("GB - United Kingdom"), searchEngine.search(this))
        }
    }

    @Test
    fun `+ combines only duplicated, results returns value 3`() {
        "+united+kingdom+".apply {
            assertEquals(listOf("GB - United Kingdom"), searchEngine.search(this))
        }
    }

    @Test
    fun `+ combines only duplicated, results no value 1`() {
        "viet+united+kingdom".apply {
            assertEquals(emptyList<String>(), searchEngine.search(this))
        }
    }

    @Test
    fun `+ combines only duplicated, results no value 2`() {
        "united+kingdom+viet".apply {
            assertEquals(emptyList<String>(), searchEngine.search(this))
        }
    }

}