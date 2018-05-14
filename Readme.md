A sample of RxKotlin used in Search box.

## Inspiration 
- Inspired by https://www.raywenderlich.com/170233/reactive-programming-rxandroid-kotlin-introduction (follow steps by steps from 1 to 5)

## Additional references to improve the SearchBox behavior
- https://blog.mindorks.com/implement-search-using-rxjava-operators-c8882b64fe1d (`distinctUntilChanged()`)
- https://medium.com/@amanshuraikwar.in/dont-use-observable-fromcallable-b36b32cb278 (`UndeliverableException` and `tryOnError`
- http://www.jayrambhia.com/notes/rxjava-debounce (RxJava Debounce)
- https://medium.com/appunite-edu-collection/rxjava-flatmap-switchmap-and-concatmap-differences-examples-6d1f3ff88ee0 (flatMap, concatMap, switchMap)

## Learnt stuffs
- define an observable (create with emitter)
- turn asynchronous events like button clicks and text field context changes into observables
- transform observable items (map, ...)
- filter observable items (filter)
- reduce backpressure with debounce(time)
- combine several observables into one (merge, concat)
- specify the thread on which code should be executed
- `switchMap`: subcribe only the last query and cancel all previous queries
- `distinctUntilChanged`: notify only when text changed from the last one 

## Feature of SearchBox
- Data list is a `countries.json` list.
- Use space to search many words, the result is a list containing all distinct elements.
- User `+` to combine search words, the results is a list containing all elements that are contained by all available results.

## Unittest
- Check `SearchEngineTest` for more detail.

