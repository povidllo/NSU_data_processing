#!/bin/bash

javac src/main/java/kuzminov/Client.java || exit 1

PORT=8081

NAMES=("Alice" "Bob" "Charlie" "Alice" "Diana")

CLIENTS=100

OUTPUT_FILE="clients_abort_false.txt"
> "$OUTPUT_FILE"

for i in $(seq 1 $CLIENTS); do
  NAME=${NAMES[$RANDOM % ${#NAMES[@]}]}

  DELAY=$((1 + RANDOM % 5))

  if (( RANDOM % 2 )); then
    ABORT="true"
  else
    ABORT="false"
    echo "$i" >> "$OUTPUT_FILE"
  fi

  echo "Запускаю клиента $i: порт=$PORT, имя=$NAME, задержка=$DELAY, abort=$ABORT"

  java -cp src/main/java kuzminov.Client "$PORT" "$NAME" "$DELAY" "$ABORT" &
done

wait
echo "Все клиенты завершили работу"
