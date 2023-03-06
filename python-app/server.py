import os
from flask import Flask, render_template
from flask_healthz import healthz
	
app = Flask(__name__)
HEALTHZ = {
    "livez": "server.checks.liveness",
    "readyz": "server.checks.readiness",
}
		
app.config['HEALTHZ'] = HEALTHZ

app.register_blueprint(healthz, url_prefix="/healthz")

def liveness():
    pass

def readiness():
    pass


@app.route('/')
def hello_world():
    return render_template('index.html')

if __name__ == '__main__':
    app.run()
