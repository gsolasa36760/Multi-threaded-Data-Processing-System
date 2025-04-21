package main

import (
	"fmt"
	"log"
	"os"
	"sync"
	"time"
)

type Task struct {
	ID int
}

func worker(id int, taskChan <-chan Task, wg *sync.WaitGroup, file *os.File, mutex *sync.Mutex) {
	defer wg.Done()
	log.Printf("Worker %d started.\n", id)

	for task := range taskChan {
		time.Sleep(100 * time.Millisecond) // Simulate processing time
		output := fmt.Sprintf("Worker %d processed Task %d\n", id, task.ID)
		fmt.Print(output)

		mutex.Lock()
		_, err := file.WriteString(output)
		mutex.Unlock()
		if err != nil {
			log.Printf("Worker %d error: %v\n", id, err)
		}
	}

	log.Printf("Worker %d finished.\n", id)
}

func main() {
	totalTasks := 20
	workerCount := 5

	taskChan := make(chan Task, totalTasks)
	var wg sync.WaitGroup
	var mutex sync.Mutex

	file, err := os.Create("output.txt")
	if err != nil {
		log.Fatalf("Failed to create file: %v", err)
	}
	defer file.Close()

	// Start workers
	for i := 1; i <= workerCount; i++ {
		wg.Add(1)
		go worker(i, taskChan, &wg, file, &mutex)
	}

	// Send tasks
	for i := 1; i <= totalTasks; i++ {
		taskChan <- Task{ID: i}
	}
	close(taskChan)

	// Wait for workers to finish
	wg.Wait()
	log.Println("All workers completed.")
}