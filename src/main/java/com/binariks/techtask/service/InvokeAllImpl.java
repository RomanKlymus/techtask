package com.binariks.techtask.service;

import com.binariks.techtask.User;
import com.binariks.techtask.repository.MongoDBRepo;
import com.binariks.techtask.repository.MySQLRepo;
import com.binariks.techtask.util.FileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class InvokeAllImpl extends AbstractTask {
    @Autowired
    public InvokeAllImpl(MongoDBRepo mongoDBRepo, MySQLRepo mySQLRepo, FileReader fileReader) {
        super(mongoDBRepo, mySQLRepo, fileReader);
    }

    @Override
    public Set<User> run() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS_NUMBER);
        super.run();
        List<Callable<Void>> tasks = List.of(this::process, this::process);
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executorService.execute(this::writeToMongoDB);
        executorService.execute(this::writeToMySQL);
        executorService.shutdown();
        return getUsersFromMap();
    }

    private Void process() {
        this.processData();
        return null;
    }
}
