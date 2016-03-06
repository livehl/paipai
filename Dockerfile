FROM progrium/busybox

RUN mkdir -p /app
WORKDIR /app
COPY ./paipai /app/
COPY ./public /app/public
EXPOSE 80

CMD ["/app/paipai"]
