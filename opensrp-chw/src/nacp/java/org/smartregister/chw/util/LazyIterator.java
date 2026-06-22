package org.smartregister.chw.util;


import static org.smartregister.chw.util.FnInterfaces.Producer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.smartregister.chw.util.FnInterfaces.*;

import timber.log.Timber;

public class LazyIterator<T> implements Iterator<T> {
    private int index=0;
    private final Producer<T> dataProvider;
    private final List<BiFunction<T,Integer,T>> operations;
    private T nextItem;
    public LazyIterator(Producer<T> dataProvider,List<BiFunction<T,Integer,T>> operations) {
        this.dataProvider = dataProvider;
        this.operations=new ArrayList<>(operations);
    }
    public LazyIterator(Producer<T> dataProvider) {
        this(dataProvider,new ArrayList<>());
    }

    public LazyIterator<T> copy(){
        return new LazyIterator<>(dataProvider,operations);
    }

    @Override
    public boolean hasNext() {
        if(nextItem!=null){ return true; }
        try {
            T data;
            do{
                data= dataProvider.produce();
                if(data==null){return false;}
                data=processItem(data);
            }while(data==null);
            nextItem=data;
            return true;
        }
        catch (NoSuchElementException|IndexOutOfBoundsException  e) { return false;}
        catch (Exception e) { Timber.e(e); return false;}
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();

        T current= nextItem;
        nextItem=null;
        index++;
        return current;
    }

    public T processItem(T item) {
        for (BiFunction<T,Integer, T> operation : operations) {
            T copy=item;
            if(copy==null){return null;}
            item = FnList.ex(() -> operation.apply(copy,index));
        }
        return item;
    }
    public void addOperation(BiFunction<T,Integer,T> operation) {
        operations.add(operation) ;
    }
}