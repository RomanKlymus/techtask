package com.binariks.techtask.service;

import com.binariks.techtask.User;
import com.binariks.techtask.repository.MongoDBRepo;
import com.binariks.techtask.repository.MySQLRepo;
import com.binariks.techtask.util.FileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class IsTerminatedImpl extends AbstractTask {
    @Autowired
    public IsTerminatedImpl(MongoDBRepo mongoDBRepo, MySQLRepo mySQLRepo, FileReader fileReader) {
        super(mongoDBRepo, mySQLRepo, fileReader);
    }

    @Override
    public Set<User> run() {
        super.run();
        ExecutorService executorService = Executors.newFixedThreadPool(THREADS_NUMBER);
        ExecutorService writers = Executors.newFixedThreadPool(THREADS_NUMBER);
        executorService.execute(this::processData);
        executorService.execute(this::processData);
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            System.out.print("");
        }
        writers.execute(this::writeToMongoDB);
        writers.execute(this::writeToMySQL);
        writers.shutdown();
        while (!writers.isTerminated()) {
            System.out.print("");
        }
        return getUsersFromMap();
    }
}
