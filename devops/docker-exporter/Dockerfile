FROM python:3.12-alpine

WORKDIR /app
RUN pip install --no-cache-dir prometheus_client==0.21.1 docker==7.1.0
COPY exporter.py .

EXPOSE 9487
CMD ["python", "-u", "exporter.py"]
