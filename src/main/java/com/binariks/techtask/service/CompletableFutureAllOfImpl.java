package com.binariks.techtask.service;

import com.binariks.techtask.User;
import com.binariks.techtask.repository.MongoDBRepo;
import com.binariks.techtask.repository.MySQLRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CompletableFutureAllOfImpl implements TaskService {

    private static final String FILE_NAME = "input.txt";
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Integer> users = new HashMap<>();
    private final MongoDBRepo mongoDBRepo;
    private final MySQLRepo mySQLRepo;
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    public CompletableFutureAllOfImpl(MongoDBRepo mongoDBRepo, MySQLRepo mySQLRepo) {
        this.mongoDBRepo = mongoDBRepo;
        this.mySQLRepo = mySQLRepo;
    }

    @Override
    public Set<User> run() {
        readFile();
        users.clear();
        CompletableFuture<Void> future = CompletableFuture
                .allOf(CompletableFuture.runAsync(this::processData), CompletableFuture.runAsync(this::processData));
        future.thenRunAsync(this::writeToMongoDB);
        future.thenRunAsync(this::writeToMySQL);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return getUsersFromMap();
    }

    private void writeToMongoDB() {
        mongoDBRepo.saveAll(getUsersFromMap());
    }

    private void writeToMySQL() {
        mySQLRepo.saveAll(getUsersFromMap());
    }

    private Set<User> getUsersFromMap() {
        return users.entrySet().stream().map(entry -> new User(entry.getKey(), entry.getValue())).collect(Collectors.toSet());
    }

    private void processData() {
        String str;
        String[] strings;
        while (!queue.isEmpty()) {
            str = queue.poll();
            if (str != null) {
                strings = str.split(",");
                String name = strings[1];
                Integer value = Integer.valueOf(strings[2]);
                lock.lock();
                try {
                    users.put(name, users.containsKey(name) ? users.get(name) + value : value);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private void readFile() {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(FILE_NAME);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found");
        }
        try (Scanner scanner = new Scanner(inputStream)) {
            while (scanner.hasNextLine()) {
                queue.add(scanner.nextLine());
            }
        }
    }

}